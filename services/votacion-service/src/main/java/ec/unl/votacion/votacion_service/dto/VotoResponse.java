package ec.unl.votacion.votacion_service.dto;

import java.time.Instant;

public record VotoResponse(
        Long id,
        Long votacionId,
        Long opcionId,
        Long usuarioId,
        Instant emitidoEn,
        String mensaje
) {
}
