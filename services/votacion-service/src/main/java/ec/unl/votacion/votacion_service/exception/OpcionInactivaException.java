package ec.unl.votacion.votacion_service.exception;

public class OpcionInactivaException extends VotacionDomainException {

    public OpcionInactivaException(Long id) {
        super("La opcion " + id + " no esta activa");
    }
}
