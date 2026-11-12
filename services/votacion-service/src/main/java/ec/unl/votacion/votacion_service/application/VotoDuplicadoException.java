package ec.unl.votacion.votacion_service.application;

public class VotoDuplicadoException extends RuntimeException {
    public VotoDuplicadoException(String message) {
        super(message);
    }
}
