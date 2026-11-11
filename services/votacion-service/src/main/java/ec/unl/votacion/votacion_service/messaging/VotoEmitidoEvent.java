package ec.unl.votacion.votacion_service.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record VotoEmitidoEvent(
        UUID eventId,
        Integer schemaVersion,
        Long votoId,
        Long eleccionId,
        Long candidatoId,
        Long idUsuario,
        OffsetDateTime occurredAt
) {
}
