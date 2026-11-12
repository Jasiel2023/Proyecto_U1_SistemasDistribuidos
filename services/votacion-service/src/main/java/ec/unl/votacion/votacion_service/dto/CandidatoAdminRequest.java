package ec.unl.votacion.votacion_service.dto;

import jakarta.validation.constraints.NotBlank;

public record CandidatoAdminRequest(
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotBlank String partidoPolitico
) {
}
