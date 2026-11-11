package ec.unl.votacion.resultados;

import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class TestEventos {

    private TestEventos() {
    }

    public static VotoEmitidoEvent voto(Long eleccionId, Long candidatoId) {
        return new VotoEmitidoEvent(UUID.randomUUID(), 1, System.nanoTime(), eleccionId,
                candidatoId, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static VotoEmitidoEvent voto(UUID eventId, Long votoId, Long eleccionId, Long candidatoId) {
        return new VotoEmitidoEvent(eventId, 1, votoId, eleccionId, candidatoId,
                null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
