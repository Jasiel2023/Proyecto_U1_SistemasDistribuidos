package ec.unl.votacion.votacion_service.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record EmitirVotoRequest(
        @NotNull UUID eleccionId,
        @NotNull UUID candidatoId,
        @NotNull @Positive Integer votanteId) {
}
