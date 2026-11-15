package bo.onesweb.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SolicitudReporte(
    @NotNull(message = "idCliente es obligatorio")
    UUID idCliente,

    @NotBlank(message = "La categoria es obligatoria")
    @Size(max = 80, message = "La categoria admite hasta 80 caracteres")
    String categoria,

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 2000, message = "La descripcion admite hasta 2000 caracteres")
    String descripcion,

    @NotNull(message = "La fecha es obligatoria")
    OffsetDateTime fecha,

    @NotNull(message = "La ubicacion es obligatoria")
    @Valid
    UbicacionReporte ubicacion,

    @NotEmpty(message = "Debe adjuntar al menos una evidencia")
    @Size(max = 3, message = "Se permiten hasta 3 evidencias")
    @Valid
    @JsonDeserialize(using = SolicitudReporte.LectorEvidencias.class)
    List<@NotNull(message = "La evidencia no puede ser nula")
        EvidenciaReporte> evidencias
) {
    public static class LectorEvidencias
            extends JsonDeserializer<List<EvidenciaReporte>> {

        @Override
        public List<EvidenciaReporte> deserialize(
                JsonParser lector, DeserializationContext contexto)
                throws IOException {

            if (!lector.isExpectedStartArrayToken()) {
                throw JsonMappingException.from(
                    lector, "evidencias debe ser una lista");
            }

            List<EvidenciaReporte> resultado = new ArrayList<>(3);

            while (lector.nextToken() != JsonToken.END_ARRAY) {
                if (lector.currentToken() != JsonToken.START_OBJECT) {
                    throw JsonMappingException.from(
                        lector, "Cada evidencia debe ser un objeto JSON");
                }
                if (resultado.size() >= 3) {
                    throw JsonMappingException.from(
                        lector, "Se permiten hasta 3 evidencias");
                }
                resultado.add(
                    contexto.readValue(lector, EvidenciaReporte.class));
            }

            return List.copyOf(resultado);
        }
    }
}
