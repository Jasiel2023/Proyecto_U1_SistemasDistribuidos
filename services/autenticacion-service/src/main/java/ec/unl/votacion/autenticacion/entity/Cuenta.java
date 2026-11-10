package ec.unl.votacion.autenticacion.entity;

import jakarta.persistence.*;

/**
 * Entidad Cuenta con relación 1-1 a Sesion.
 * Almacena hash BCrypt de la clave.
 */
@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nickname", nullable = false, unique = true, length = 100)
    private String nickname;

    @Column(name = "clave_cifrada", nullable = false, length = 255)
    private String claveCifrada;

    @Column(name = "esta_activa", nullable = false)
    private boolean estaActiva;

    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Sesion sesion;

    public Cuenta() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getClaveCifrada() { return claveCifrada; }
    public void setClaveCifrada(String claveCifrada) { this.claveCifrada = claveCifrada; }

    public boolean isEstaActiva() { return estaActiva; }
    public void setEstaActiva(boolean estaActiva) { this.estaActiva = estaActiva; }

    public Sesion getSesion() { return sesion; }
    public void setSesion(Sesion sesion) { this.sesion = sesion; }

    /**
     * Método de dominio para autenticar, delegado a servicio de seguridad en la práctica.
     */
    public boolean autenticar(String nickname, String clave) {
        // Implementación referencial; uso real con BCrypt en servicio
        return this.nickname != null && this.nickname.equals(nickname);
    }
}
