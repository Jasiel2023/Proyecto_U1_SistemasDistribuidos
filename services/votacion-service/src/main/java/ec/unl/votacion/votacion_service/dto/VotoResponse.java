package ec.unl.votacion.votacion_service.dto;

import java.util.UUID;

import java.time.Instant;

public record VotoResponse(
        UUID id,
        UUID eleccionId,
        UUID candidatoId,
        UUID idUsuario,
        Instant emitidoEn,
        String mensaje
) {
}
