package ec.unl.votacion.autenticacion.messaging.event;

public record CuentaCrearEvent(
        String nickname,
        String clave,
        String correo,
        String rol
) {
}
