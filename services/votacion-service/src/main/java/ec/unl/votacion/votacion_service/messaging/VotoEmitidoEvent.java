package ec.unl.votacion.votacion_service.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VotoEmitidoEvent(
        UUID eventId,
        Integer schemaVersion,
        UUID votoId,
        UUID eleccionId,
        UUID candidatoId,
        OffsetDateTime occurredAt) {
}
