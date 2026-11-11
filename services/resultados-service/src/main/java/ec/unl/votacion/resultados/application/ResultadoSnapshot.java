package ec.unl.votacion.resultados.application;

import java.time.OffsetDateTime;
import java.util.List;

public record ResultadoSnapshot(
        Long eleccionId,
        long totalVotos,
        List<ConteoSnapshot> conteos,
        long revision,
        OffsetDateTime ultimaActualizacion,
        boolean disponible) {

    public static ResultadoSnapshot vacio(Long eleccionId) {
        return new ResultadoSnapshot(eleccionId, 0L, List.of(), 0L, null, false);
    }

        public record ConteoSnapshot(Long candidatoId, long votos) {
    }
}
