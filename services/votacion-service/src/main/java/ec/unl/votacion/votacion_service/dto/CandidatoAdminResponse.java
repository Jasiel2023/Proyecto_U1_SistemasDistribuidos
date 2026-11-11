package ec.unl.votacion.votacion_service.dto;

import java.util.UUID;

public record CandidatoAdminResponse(
        UUID id,
        String nombre,
        String apellido,
        String partidoPolitico,
        boolean activo
) {
}
