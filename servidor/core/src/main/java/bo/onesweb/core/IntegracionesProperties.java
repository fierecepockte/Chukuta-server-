package bo.onesweb.core;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integraciones")
public record IntegracionesProperties(
    Duration timeoutConexion,
    Duration timeoutRespuesta,
    Map<String, Servicio> servicios
) {
    public record Servicio(String url) {
    }
}
