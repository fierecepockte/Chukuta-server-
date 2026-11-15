package bo.onesweb.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class LimitesEntradaConfig {

    private static final long MAX_DOCUMENTO = 10L * 1024 * 1024;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonEstrictoCore() {
        return builder -> builder
            .featuresToEnable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .postConfigurer(mapper ->
                mapper.getFactory().setStreamReadConstraints(
                    StreamReadConstraints.builder()
                        .maxDocumentLength(MAX_DOCUMENTO)
                        .maxStringLength(4 * 1024 * 1024)
                        .maxNumberLength(64)
                        .maxNestingDepth(32)
                        .build()));
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> limitarTrabajoCore(
            ObjectMapper json) {

        Semaphore cupos = new Semaphore(4);

        OncePerRequestFilter filtro = new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest peticion) {
                return "GET".equals(peticion.getMethod())
                    && "/api/health".equals(peticion.getRequestURI());
            }

            @Override
            protected void doFilterInternal(HttpServletRequest peticion,
                    HttpServletResponse respuesta, FilterChain cadena)
                    throws ServletException, IOException {

                if (peticion.getContentLengthLong() > MAX_DOCUMENTO) {
                    responder(respuesta, json, 413,
                        "solicitud_demasiado_grande",
                        "La solicitud supera 10 MiB");
                    return;
                }

                if (!cupos.tryAcquire()) {
                    respuesta.setHeader("Retry-After", "1");
                    responder(respuesta, json, 503, "core_ocupado",
                        "El core alcanzo su limite de solicitudes simultaneas");
                    return;
                }

                try {
                    cadena.doFilter(peticion, respuesta);
                } finally {
                    cupos.release();
                }
            }
        };

        FilterRegistrationBean<OncePerRequestFilter> registro =
            new FilterRegistrationBean<>(filtro);
        registro.setName("limiteTrabajoCore");
        registro.addUrlPatterns("/*");
        registro.setDispatcherTypes(DispatcherType.REQUEST);
        registro.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registro;
    }

    private static void responder(HttpServletResponse respuesta,
            ObjectMapper json, int codigo, String error, String mensaje)
            throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        json.writeValue(respuesta.getOutputStream(), Map.of(
            "codigo", codigo,
            "error", error,
            "mensaje", mensaje,
            "detalles", List.of()
        ));
    }
}
