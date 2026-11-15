package bo.onesweb.core;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CatalogoServicios {

    private final IntegracionesProperties configuracion;

    public CatalogoServicios(IntegracionesProperties configuracion) {
        this.configuracion = configuracion;
    }

    public URI obtenerUrl(String nombre) {
        var servicios = configuracion.servicios();
        var servicio = servicios == null ? null : servicios.get(nombre);

        if (servicio == null || servicio.url() == null
                || servicio.url().isBlank()) {
            throw new IntegracionException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "servicio_no_configurado",
                "Todavia no se ha configurado el servicio: " + nombre
            );
        }

        try {
            URI url = URI.create(servicio.url().trim());

            if (!("http".equals(url.getScheme())
                    || "https".equals(url.getScheme()))
                    || url.getHost() == null
                    || url.getUserInfo() != null
                    || url.getQuery() != null
                    || url.getFragment() != null) {
                throw new IllegalArgumentException();
            }

            return url;
        } catch (IllegalArgumentException excepcion) {
            throw new IntegracionException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "configuracion_invalida",
                "Revise la direccion configurada del servicio: " + nombre
            );
        }
    }
}
