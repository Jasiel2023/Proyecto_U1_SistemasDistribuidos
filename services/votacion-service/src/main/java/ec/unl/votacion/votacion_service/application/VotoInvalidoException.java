package ec.unl.votacion.votacion_service.application;

public class VotoInvalidoException extends RuntimeException {
    public VotoInvalidoException(String message) {
        super(message);
    }
}
