package ec.unl.votacion.votacion_service.application;

public class EleccionNoEncontradaException extends RuntimeException {
    public EleccionNoEncontradaException(String message) {
        super(message);
    }
}
