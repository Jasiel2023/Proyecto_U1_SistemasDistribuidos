package ec.unl.votacion.votacion_service.messaging;

import java.time.Instant;
import java.util.UUID;

public record VotoEmitidoEvent(
        UUID eventoId,
        Long votoId,
        Long eleccionId,
        Long candidatoId,
        Long idUsuario,
        Instant emitidoEn
) {
}
