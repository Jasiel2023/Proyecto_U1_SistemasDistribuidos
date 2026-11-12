package ec.unl.votacion.resultados.messaging;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VotoEmitidoEvent(
        @NotNull UUID eventId,
        @NotNull @Min(1) @Max(1) Integer schemaVersion,
        @NotNull UUID votoId,
        @NotNull UUID eleccionId,
        @NotNull UUID candidatoId,
        UUID idUsuario,
        @NotNull OffsetDateTime occurredAt
) {
}
