package bo.onesweb.core;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UbicacionReporte(

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90", message = "La latitud minima es -90")
    @DecimalMax(value = "90", message = "La latitud maxima es 90")
    BigDecimal latitud,

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180", message = "La longitud minima es -180")
    @DecimalMax(value = "180", message = "La longitud maxima es 180")
    BigDecimal longitud,

    @NotNull(message = "La precision es obligatoria")
    @DecimalMin(value = "0", message = "La precision no puede ser negativa")
    @DecimalMax(
        value = "10",
        inclusive = false,
        message = "La precision debe ser menor a 10 metros"
    )
    BigDecimal precisionMetros

) {
}
