package ec.unl.votacion.votacion_service.exception;

public class OpcionNoEncontradaException extends VotacionDomainException {

    public OpcionNoEncontradaException(Long id, Long votacionId) {
        super("No existe la opcion " + id + " para la votacion " + votacionId);
    }
}
