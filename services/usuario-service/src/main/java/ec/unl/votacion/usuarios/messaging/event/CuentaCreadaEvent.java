package ec.unl.votacion.usuarios.messaging.event;

public record CuentaCreadaEvent(
        Integer idCuenta,
        String nickname,
        boolean exito,
        String mensaje
) {
}
