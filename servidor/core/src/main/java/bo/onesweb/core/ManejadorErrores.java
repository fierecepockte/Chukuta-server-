package bo.onesweb.core;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(IntegracionException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegracion(
        IntegracionException excepcion
    ) {
        Map<String, Object> respuesta = Map.of(
            "codigo", excepcion.getEstado().value(),
            "error", excepcion.getError(),
            "mensaje", excepcion.getMessage(),
            "detalles", List.of()
        );

        return ResponseEntity.status(excepcion.getEstado())
            .header("Cache-Control", "no-store")
            .body(respuesta);
    }
}
