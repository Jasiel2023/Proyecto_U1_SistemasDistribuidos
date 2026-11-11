package ec.unl.votacion.votacion_service.dto;

import java.util.UUID;

public record CandidatoResponse(
        UUID id,
        String nombre,
        String apellido,
        String partidoPolitico
) {
}
