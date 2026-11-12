package ec.unl.votacion.votacion_service.exception;

import java.util.UUID;

public class VotoDuplicadoException extends EleccionDomainException {

    public VotoDuplicadoException(UUID idUsuario, UUID eleccionId) {
        super("El usuario " + idUsuario + " ya voto en la eleccion " + eleccionId);
    }
}
