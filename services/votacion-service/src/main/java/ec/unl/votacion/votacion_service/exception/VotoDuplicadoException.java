package ec.unl.votacion.votacion_service.exception;

public class VotoDuplicadoException extends VotacionDomainException {

    public VotoDuplicadoException(Long usuarioId, Long votacionId) {
        super("El usuario " + usuarioId + " ya voto en la votacion " + votacionId);
    }
}
