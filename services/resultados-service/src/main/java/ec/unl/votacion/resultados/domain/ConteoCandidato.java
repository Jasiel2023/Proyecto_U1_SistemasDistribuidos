package ec.unl.votacion.resultados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "resultado_conteo")
public class ConteoCandidato {

    @EmbeddedId
    private ConteoCandidatoId id;

    @Column(nullable = false)
    private long votos;

    @Column(name = "ultima_actualizacion", nullable = false)
    private OffsetDateTime ultimaActualizacion;

    protected ConteoCandidato() {
    }

    public ConteoCandidatoId getId() {
        return id;
    }

    public long getVotos() {
        return votos;
    }

    public OffsetDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }
}
