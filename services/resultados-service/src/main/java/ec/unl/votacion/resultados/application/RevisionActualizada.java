package ec.unl.votacion.resultados.application;

import java.time.OffsetDateTime;

public record RevisionActualizada(long revision, long totalVotos, OffsetDateTime ultimaActualizacion) {
}
