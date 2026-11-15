package bo.onesweb.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain filtroSeguridadCore(
            HttpSecurity http, JwtDecoder decoder, ObjectMapper json)
            throws Exception {

        AuthenticationEntryPoint noAutenticado = (peticion, respuesta, error) ->
            responder(respuesta, json, 401, "no_autenticado",
                "Se requiere un token de acceso valido");

        AccessDeniedHandler sinPermiso = (peticion, respuesta, error) ->
            responder(respuesta, json, 403, "acceso_denegado",
                "No tiene permiso para esta operacion");

        return http
            // API con tokens en Authorization, sin autenticacion por cookies.
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reglas -> reglas
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reportes")
                    .hasAuthority("SCOPE_reportes:leer:todos")
                .requestMatchers(HttpMethod.POST, "/api/reportes")
                    .hasAuthority("SCOPE_reportes:crear")
                .anyRequest().denyAll())
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint(noAutenticado)
                .accessDeniedHandler(sinPermiso))
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.decoder(decoder))
                .authenticationEntryPoint(noAutenticado)
                .accessDeniedHandler(sinPermiso))
            .build();
    }

    private void responder(HttpServletResponse respuesta, ObjectMapper json,
            int codigo, String error, String mensaje) throws IOException {
        respuesta.setStatus(codigo);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.setHeader("Cache-Control", "no-store");
        if (codigo == 401) {
            respuesta.setHeader("WWW-Authenticate", "Bearer");
        }
        json.writeValue(respuesta.getOutputStream(), Map.of(
            "codigo", codigo,
            "error", error,
            "mensaje", mensaje,
            "detalles", List.of()
        ));
    }
}
