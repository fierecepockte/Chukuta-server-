package bo.onesweb.core;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorValidacion {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> camposInvalidos(
        MethodArgumentNotValidException excepcion
    ) {
        var detalles = excepcion.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> Map.of(
                "campo", error.getField(),
                "problema", Objects.requireNonNullElse(
                    error.getDefaultMessage(), "Valor no valido"
                )
            ))
            .toList();

        return respuesta(
            "El reporte no cumple las reglas de validacion",
            detalles
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(
        HttpMessageNotReadableException excepcion
    ) {
        return respuesta(
            "El cuerpo debe ser un JSON valido con los tipos de datos esperados",
            List.of()
        );
    }

    private ResponseEntity<Map<String, Object>> respuesta(
        String mensaje,
        List<Map<String, String>> detalles
    ) {
        return ResponseEntity.badRequest()
            .header("Cache-Control", "no-store")
            .body(Map.of(
                "codigo", 400,
                "error", "datos_mal_formados",
                "mensaje", mensaje,
                "detalles", detalles
            ));
    }
}
