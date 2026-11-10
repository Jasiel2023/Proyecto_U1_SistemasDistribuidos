package ec.unl.votacion.votacion_service.exception;

public class VotoDuplicadoException extends EleccionDomainException {

    public VotoDuplicadoException(Long idUsuario, Long eleccionId) {
        super("El usuario " + idUsuario + " ya voto en la eleccion " + eleccionId);
    }
}
