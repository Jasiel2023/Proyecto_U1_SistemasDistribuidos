package ec.unl.votacion.votacion_service.exception;

public abstract class EleccionDomainException extends RuntimeException {

    protected EleccionDomainException(String message) {
        super(message);
    }
}
