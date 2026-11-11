package ec.unl.votacion.autenticacion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad Sesion representa la sesión activa de una cuenta.
 * Relación 1-1 con Cuenta.
 */
@Entity
@Table(name = "sesion")
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_token", nullable = false, length = 20)
    private EstadoToken estadoToken;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    private Cuenta cuenta;

    public Sesion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }

    public EstadoToken getEstadoToken() { return estadoToken; }
    public void setEstadoToken(EstadoToken estadoToken) { this.estadoToken = estadoToken; }

    public Cuenta getCuenta() { return cuenta; }
    public void setCuenta(Cuenta cuenta) { this.cuenta = cuenta; }
}
