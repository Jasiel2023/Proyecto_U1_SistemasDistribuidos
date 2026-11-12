package ec.unl.votacion.votacion_service.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EleccionResponse(
        UUID id,
        String titulo,
        String descripcion,
        String estado,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        List<CandidatoResponse> candidatos) {

    public record CandidatoResponse(UUID id, String nombre, String partido) {
    }
}
