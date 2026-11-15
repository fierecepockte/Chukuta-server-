package bo.onesweb.core;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient clienteHttpServicios(IntegracionesProperties configuracion) {
        return HttpClient.newBuilder()
            .connectTimeout(configuracion.timeoutConexion())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    }
}
