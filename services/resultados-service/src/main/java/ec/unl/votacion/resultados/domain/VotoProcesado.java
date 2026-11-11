package ec.unl.votacion.resultados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "voto_procesado")
public class VotoProcesado {
    @Id @Column(name = "event_id", nullable = false) private UUID eventId;
    @Column(name = "voto_id", nullable = false) private Long votoId;
    @Column(name = "eleccion_id", nullable = false) private Long eleccionId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Column(name = "procesado_en", nullable = false) private OffsetDateTime procesadoEn;

    protected VotoProcesado() {}

    public VotoProcesado(UUID eventId, Long votoId, Long eleccionId, Long candidatoId,
                         OffsetDateTime occurredAt, OffsetDateTime procesadoEn) {
        this.eventId = eventId; this.votoId = votoId; this.eleccionId = eleccionId;
        this.candidatoId = candidatoId; this.occurredAt = occurredAt; this.procesadoEn = procesadoEn;
    }
    public UUID getEventId() { return eventId; }
    public Long getVotoId() { return votoId; }
    public Long getEleccionId() { return eleccionId; }
    public Long getCandidatoId() { return candidatoId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public OffsetDateTime getProcesadoEn() { return procesadoEn; }
}
