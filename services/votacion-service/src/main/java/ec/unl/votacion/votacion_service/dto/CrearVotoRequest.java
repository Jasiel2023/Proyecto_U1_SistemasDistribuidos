package ec.unl.votacion.votacion_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearVotoRequest(
        @NotNull @Positive Long eleccionId,
        @NotNull @Positive Long candidatoId,
        @NotNull @Positive Long idUsuario
) {
}
