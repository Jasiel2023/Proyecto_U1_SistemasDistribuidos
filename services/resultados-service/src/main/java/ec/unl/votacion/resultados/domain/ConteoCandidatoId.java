package ec.unl.votacion.resultados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ConteoCandidatoId implements Serializable {
    @Column(name = "eleccion_id", nullable = false) private Long eleccionId;
    @Column(name = "candidato_id", nullable = false) private Long candidatoId;
    protected ConteoCandidatoId() {}
    public ConteoCandidatoId(Long eleccionId, Long candidatoId) { this.eleccionId = eleccionId; this.candidatoId = candidatoId; }
    public Long getEleccionId() { return eleccionId; }
    public Long getCandidatoId() { return candidatoId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof ConteoCandidatoId that)) return false; return Objects.equals(eleccionId, that.eleccionId) && Objects.equals(candidatoId, that.candidatoId); }
    @Override public int hashCode() { return Objects.hash(eleccionId, candidatoId); }
}
