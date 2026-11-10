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
import java.time.LocalTime;

@Entity
@Table(name = "votos")
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false)
    private Eleccion eleccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Candidato candidato;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    protected Voto() {
    }

    public Voto(Eleccion eleccion, Candidato candidato, Long idUsuario) {
        this.eleccion = eleccion;
        this.candidato = candidato;
        this.idUsuario = idUsuario;
        this.hora = LocalTime.now();
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

    public Eleccion getEleccion() {
        return eleccion;
    }

    public Candidato getCandidato() {
        return candidato;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public LocalTime getHora() {
        return hora;
    }

    public Instant getEmitidoEn() {
        return emitidoEn;
    }
}
