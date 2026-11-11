package ec.unl.votacion.resultados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_conflicto")
public class EventoConflicto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 40)
    private String motivo;

    @Column(nullable = false, length = 500)
    private String detalle;

    @Column(name = "recibido_en", nullable = false)
    private OffsetDateTime recibidoEn;

    protected EventoConflicto() {
    }

    public EventoConflicto(UUID eventId, String motivo, String detalle, OffsetDateTime recibidoEn) {
        this.eventId = eventId;
        this.motivo = motivo;
        this.detalle = detalle;
        this.recibidoEn = recibidoEn;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public OffsetDateTime getRecibidoEn() {
        return recibidoEn;
    }
}
