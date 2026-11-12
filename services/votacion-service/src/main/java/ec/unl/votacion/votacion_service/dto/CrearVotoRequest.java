package ec.unl.votacion.votacion_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CrearVotoRequest(
        @NotNull UUID eleccionId,
        @NotNull UUID candidatoId
) {
}
