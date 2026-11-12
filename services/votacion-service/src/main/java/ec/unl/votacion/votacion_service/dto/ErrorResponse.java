package ec.unl.votacion.votacion_service.dto;

import java.time.Instant;

public record ErrorResponse(
        int status,
        String code,
        String message,
        Instant timestamp
) {
}
