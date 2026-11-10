package ec.unl.votacion.votacion_service.exception;

public class EleccionNoEncontradaException extends EleccionDomainException {

    public EleccionNoEncontradaException(Long id) {
        super("No existe la eleccion con id " + id);
    }
}
