package ec.unl.votacion.votacion_service.exception;

import java.util.UUID;

public class CandidatoInactivoException extends EleccionDomainException {

    public CandidatoInactivoException(UUID id) {
        super("El candidato " + id + " no esta activo");
    }
}
