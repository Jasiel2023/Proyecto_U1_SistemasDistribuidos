package ec.unl.votacion.votacion_service.dto;

public record CandidatoAdminResponse(
        Long id,
        String nombre,
        String apellido,
        String partidoPolitico,
        boolean activo
) {
}
