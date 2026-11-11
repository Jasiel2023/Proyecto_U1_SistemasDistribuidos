package ec.unl.votacion.votacion_service.exception;

import java.util.UUID;

public class EleccionNoEncontradaException extends EleccionDomainException {

    public EleccionNoEncontradaException(UUID id) {
        super("No existe la eleccion con id " + id);
    }
}
