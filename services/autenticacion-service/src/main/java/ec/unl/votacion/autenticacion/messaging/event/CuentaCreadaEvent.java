package ec.unl.votacion.autenticacion.messaging.event;

public record CuentaCreadaEvent(
        Integer idCuenta,
        String nickname,
        boolean exito,
        String mensaje
) {
}
