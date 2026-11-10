package ec.unl.votacion.votacion_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "votos")
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "votacion_id", nullable = false)
    private Votacion votacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opcion_id", nullable = false)
    private Opcion opcion;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    protected Voto() {
    }

    public Voto(Votacion votacion, Opcion opcion, Long usuarioId) {
        this.votacion = votacion;
        this.opcion = opcion;
        this.usuarioId = usuarioId;
        this.emitidoEn = Instant.now();
    }

    @PrePersist
    void asignarFechaEmision() {
        if (emitidoEn == null) {
            emitidoEn = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Votacion getVotacion() {
        return votacion;
    }

    public Opcion getOpcion() {
        return opcion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getEmitidoEn() {
        return emitidoEn;
    }
}
