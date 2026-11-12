package ec.unl.votacion.votacion_service.exception;

import java.util.UUID;

public class EleccionCerradaException extends EleccionDomainException {

    public EleccionCerradaException(UUID id) {
        super("La eleccion " + id + " no esta activa");
    }
}
