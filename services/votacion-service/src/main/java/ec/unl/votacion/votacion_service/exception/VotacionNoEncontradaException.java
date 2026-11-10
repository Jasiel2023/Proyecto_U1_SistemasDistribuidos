package ec.unl.votacion.votacion_service.exception;

public class VotacionNoEncontradaException extends VotacionDomainException {

    public VotacionNoEncontradaException(Long id) {
        super("No existe la votacion con id " + id);
    }
}
