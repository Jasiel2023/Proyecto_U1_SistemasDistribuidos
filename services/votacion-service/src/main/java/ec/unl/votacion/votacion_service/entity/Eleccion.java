package ec.unl.votacion.votacion_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "elecciones")
public class Eleccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "esta_activa", nullable = false)
    private boolean estaActiva;

    protected Eleccion() {
    }

    public Eleccion(String titulo, LocalDate fecha, LocalTime horaInicio,
                    LocalTime horaFin, boolean estaActiva) {
        this.titulo = titulo;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estaActiva = estaActiva;
    }

    public void actualizar(String titulo, LocalDate fecha, LocalTime horaInicio,
                           LocalTime horaFin, boolean estaActiva) {
        this.titulo = titulo;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estaActiva = estaActiva;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public boolean estaActiva() {
        return estaActiva;
    }

    public boolean estaAbierta() {
        return estaActiva;
    }
}
