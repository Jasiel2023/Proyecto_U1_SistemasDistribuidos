package ec.unl.votacion.votacion_service.exception;

public class EleccionCerradaException extends EleccionDomainException {

    public EleccionCerradaException(Long id) {
        super("La eleccion " + id + " no esta activa");
    }
}
