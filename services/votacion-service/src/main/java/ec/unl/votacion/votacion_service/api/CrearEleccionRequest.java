package ec.unl.votacion.votacion_service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearEleccionRequest(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 1000) String descripcion,
        @NotEmpty @Size(min = 2) List<@Valid CrearCandidatoRequest> candidatos) {
}
