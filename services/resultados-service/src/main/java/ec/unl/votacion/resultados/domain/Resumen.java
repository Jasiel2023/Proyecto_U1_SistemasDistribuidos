package ec.unl.votacion.resultados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "resultado_resumen")
public class Resumen {
    @Id @Column(name = "eleccion_id", nullable = false) private Long eleccionId;
    @Column(name = "total_votos", nullable = false) private long totalVotos;
    @Column(nullable = false) private long revision;
    @Column(name = "ultima_actualizacion", nullable = false) private OffsetDateTime ultimaActualizacion;
    protected Resumen() {}
    public Long getEleccionId() { return eleccionId; }
    public long getTotalVotos() { return totalVotos; }
    public long getRevision() { return revision; }
    public OffsetDateTime getUltimaActualizacion() { return ultimaActualizacion; }
}
