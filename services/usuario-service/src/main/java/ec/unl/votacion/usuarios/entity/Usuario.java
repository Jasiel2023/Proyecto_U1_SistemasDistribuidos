package ec.unl.votacion.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(
        name = "usuario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_usuario_id_cuenta", columnNames = "id_cuenta"),
                @UniqueConstraint(name = "uk_usuario_cedula", columnNames = "cedula"),
                @UniqueConstraint(name = "uk_usuario_correo", columnNames = "correo"),
                @UniqueConstraint(name = "uk_usuario_nickname", columnNames = "nickname")
        }
)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_cuenta", nullable = false)
    private Integer idCuenta;

    @NotBlank
    @Size(max = 13)
    @Column(name = "cedula", nullable = false, length = 13)
    private String cedula;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(name = "correo", nullable = false, length = 150)
    private String correo;

    @NotBlank
    @Size(max = 60)
    @Column(name = "nickname", nullable = false, length = 60)
    private String nickname;

    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Size(max = 100)
    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol = "VOTANTE";

    public Usuario() {}

    public Usuario(Integer idCuenta, String cedula, String correo, String nickname, String nombre, String apellido) {
        this.idCuenta = idCuenta;
        this.cedula = cedula;
        this.correo = correo;
        this.nickname = nickname;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCuenta() {
        return idCuenta;
    }

    public void setIdCuenta(Integer idCuenta) {
        this.idCuenta = idCuenta;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}
