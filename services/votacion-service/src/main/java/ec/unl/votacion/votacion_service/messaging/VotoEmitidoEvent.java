package ec.unl.votacion.votacion_service.messaging;

import java.time.Instant;
import java.util.UUID;

public record VotoEmitidoEvent(
        UUID eventoId,
        Long votoId,
        Long votacionId,
        Long opcionId,
        Long usuarioId,
        Instant emitidoEn
) {
}
