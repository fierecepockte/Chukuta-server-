package bo.onesweb.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportesController {

    private final ClienteServicios cliente;
    private final ObjectMapper json;

    public ReportesController(ClienteServicios cliente, ObjectMapper json) {
        this.cliente = cliente;
        this.json = json;
    }

    @GetMapping(value = "/api/reportes", produces = "application/json")
    public ResponseEntity<JsonNode> listar() {
        var respuesta = cliente.consultar("reportes");
        int codigo = respuesta.statusCode();

        if (codigo == 204) {
            return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
        }

        if (codigo != 200) {
            HttpStatus estado = HttpStatus.resolve(codigo);

            if (estado == null || !estado.is4xxClientError()) {
                estado = HttpStatus.BAD_GATEWAY;
            }

            throw new IntegracionException(
                estado,
                "consulta_rechazada",
                "El servicio de reportes no pudo completar la consulta"
            );
        }

        try {
            JsonNode datos = json.readTree(respuesta.body());

            if (datos == null) {
                throw new IOException();
            }

            return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(datos);
        } catch (IOException excepcion) {
            throw new IntegracionException(
                HttpStatus.BAD_GATEWAY,
                "respuesta_invalida",
                "El servicio de reportes no devolvio un JSON valido"
            );
        }
    }
}
