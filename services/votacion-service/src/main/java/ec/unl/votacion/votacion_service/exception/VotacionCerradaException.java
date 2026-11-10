package ec.unl.votacion.votacion_service.exception;

public class VotacionCerradaException extends VotacionDomainException {

    public VotacionCerradaException(Long id) {
        super("La votacion " + id + " no esta abierta");
    }
}
