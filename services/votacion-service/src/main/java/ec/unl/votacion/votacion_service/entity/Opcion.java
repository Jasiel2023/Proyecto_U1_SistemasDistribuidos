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

@Entity
@Table(name = "opciones")
public class Opcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "votacion_id", nullable = false)
    private Votacion votacion;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private boolean activa;

    protected Opcion() {
    }

    public Long getId() {
        return id;
    }

    public Votacion getVotacion() {
        return votacion;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean estaActiva() {
        return activa;
    }
}
