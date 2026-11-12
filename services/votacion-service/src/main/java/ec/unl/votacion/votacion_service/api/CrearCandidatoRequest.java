package ec.unl.votacion.votacion_service.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCandidatoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 150) String partido) {
}
