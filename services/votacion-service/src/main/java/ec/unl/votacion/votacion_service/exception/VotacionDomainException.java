package ec.unl.votacion.votacion_service.exception;

public abstract class VotacionDomainException extends RuntimeException {

    protected VotacionDomainException(String message) {
        super(message);
    }
}
