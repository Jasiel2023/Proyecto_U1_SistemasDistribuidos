package ec.unl.votacion.votacion_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "candidatos")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleccion_id", nullable = false)
    private Eleccion eleccion;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String apellido;

    @Column(name = "partido_politico", nullable = false, length = 150)
    private String partidoPolitico;

    @Column(nullable = false)
    private boolean activo;

    protected Candidato() {
    }

    public Candidato(Eleccion eleccion, String nombre, String apellido,
                     String partidoPolitico) {
        this.eleccion = eleccion;
        this.nombre = nombre;
        this.apellido = apellido;
        this.partidoPolitico = partidoPolitico;
        this.activo = true;
    }

    public UUID getId() {
        return id;
    }

    public Eleccion getEleccion() {
        return eleccion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getPartidoPolitico() {
        return partidoPolitico;
    }

    public boolean estaActivo() {
        return activo;
    }

    public boolean estaActiva() {
        return activo;
    }
}
