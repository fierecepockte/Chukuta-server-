package bo.onesweb.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrearReporteController {

    private final ClienteServicios cliente;
    private final ObjectMapper json;

    public CrearReporteController(
        ClienteServicios cliente,
        ObjectMapper json
    ) {
        this.cliente = cliente;
        this.json = json;
    }

    @PostMapping(
        value = "/api/reportes",
        consumes = "application/json",
        produces = "application/json"
    )
    public ResponseEntity<JsonNode> crear(
        @Valid @RequestBody SolicitudReporte solicitud
    ) {
        byte[] cuerpo;

        try {
            cuerpo = json.writeValueAsBytes(solicitud);
        } catch (JsonProcessingException excepcion) {
            throw new IntegracionException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "error_del_core",
                "No se pudo preparar la solicitud"
            );
        }

        var respuesta = cliente.crear("reportes-creacion", cuerpo);
        int codigo = respuesta.statusCode();

        if (codigo != 201) {
            HttpStatus estado = HttpStatus.resolve(codigo);

            if (estado == null || !estado.is4xxClientError()) {
                estado = HttpStatus.BAD_GATEWAY;
            }

            throw new IntegracionException(
                estado,
                "creacion_no_confirmada",
                "El servicio no devolvio la confirmacion esperada"
            );
        }

        try {
            JsonNode datos = json.readTree(respuesta.body());

            if (datos == null || !datos.isObject()) {
                throw new IOException();
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(datos);
        } catch (IOException excepcion) {
            throw new IntegracionException(
                HttpStatus.BAD_GATEWAY,
                "respuesta_invalida",
                "La confirmacion del servicio no contiene un objeto JSON valido"
            );
        }
    }
}
