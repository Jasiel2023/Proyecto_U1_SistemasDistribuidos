package ec.unl.votacion.votacion_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearVotoRequest(
        @NotNull @Positive Long votacionId,
        @NotNull @Positive Long opcionId,
        @NotNull @Positive Long usuarioId
) {
}
