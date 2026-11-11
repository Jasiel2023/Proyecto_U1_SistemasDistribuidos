package ec.unl.votacion.usuarios.messaging.event;

public record CuentaCrearEvent(
        String nickname,
        String clave,
        String correo,
        String rol
) {
}
