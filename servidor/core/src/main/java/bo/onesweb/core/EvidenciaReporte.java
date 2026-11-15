package bo.onesweb.core;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Base64;

public record EvidenciaReporte(
    @NotBlank(message = "El tipo de evidencia es obligatorio")
    @Size(max = 32, message = "El tipo admite hasta 32 caracteres")
    String tipo,

    @NotBlank(message = "El formato es obligatorio")
    @Size(max = 80, message = "El formato admite hasta 80 caracteres")
    @Pattern(
        regexp = "[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]*",
        message = "El formato debe ser un tipo MIME, como image/jpeg")
    String formato,

    @NotBlank(message = "El contenido de la evidencia es obligatorio")
    @Size(max = 4_194_304, message = "El texto Base64 supera el limite permitido")
    @EvidenciaReporte.Base64Limitado
    String contenidoBase64
) {
    @Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.TYPE_USE,
        ElementType.RECORD_COMPONENT
    })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = EvidenciaReporte.ValidadorBase64.class)
    public @interface Base64Limitado {
        String message() default
            "Debe contener Base64 valido y representar entre 1 byte y 3 MiB";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class ValidadorBase64
            implements ConstraintValidator<Base64Limitado, String> {

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            if (valor == null) {
                return true;
            }
            if (valor.length() > 4_194_304) {
                return false;
            }
            if (valor.isBlank()) {
                return true; // NotBlank rechaza los valores vacios.
            }

            try {
                byte[] contenido = Base64.getDecoder().decode(valor);
                return contenido.length > 0
                    && contenido.length <= 3 * 1024 * 1024;
            } catch (IllegalArgumentException error) {
                return false;
            }
        }
    }
}
