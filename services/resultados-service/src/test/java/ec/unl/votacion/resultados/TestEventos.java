package ec.unl.votacion.resultados;

import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class TestEventos {

    private TestEventos() {
    }

    public static VotoEmitidoEvent voto(UUID eleccionId, UUID candidatoId) {
        return new VotoEmitidoEvent(UUID.randomUUID(), 1, UUID.randomUUID(), eleccionId,
                candidatoId, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    public static VotoEmitidoEvent voto(UUID eventId, UUID votoId, UUID eleccionId, UUID candidatoId) {
        return new VotoEmitidoEvent(eventId, 1, votoId, eleccionId, candidatoId,
                null, OffsetDateTime.now(ZoneOffset.UTC));
    }
}
