package ec.unl.votacion.votacion_service.dto;

public record CandidatoResponse(
        Long id,
        String nombre,
        String apellido,
        String partidoPolitico
) {
}
