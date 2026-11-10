package ec.unl.votacion.votacion_service.dto;

import java.time.Instant;

public record VotoResponse(
        Long id,
        Long eleccionId,
        Long candidatoId,
        Long idUsuario,
        Instant emitidoEn,
        String mensaje
) {
}
