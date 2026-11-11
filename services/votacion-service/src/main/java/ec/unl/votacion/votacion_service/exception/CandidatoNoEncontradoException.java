package ec.unl.votacion.votacion_service.exception;

import java.util.UUID;

public class CandidatoNoEncontradoException extends EleccionDomainException {

    public CandidatoNoEncontradoException(UUID id, UUID eleccionId) {
        super("No existe el candidato " + id + " para la eleccion " + eleccionId);
    }
}
