package ec.unl.votacion.votacion_service.api;

import java.util.UUID;

public record VotoResponse(boolean aceptado, String mensaje, UUID votoId) {
}
