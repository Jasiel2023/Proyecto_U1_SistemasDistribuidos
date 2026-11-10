package ec.unl.votacion.resultados.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ResultadoSnapshot(
        UUID eleccionId,
        long totalVotos,
        List<ConteoSnapshot> conteos,
        long revision,
        OffsetDateTime ultimaActualizacion,
        boolean disponible) {

    public static ResultadoSnapshot vacio(UUID eleccionId) {
        return new ResultadoSnapshot(eleccionId, 0L, List.of(), 0L, null, false);
    }

    public record ConteoSnapshot(UUID candidatoId, long votos) {
    }
}
