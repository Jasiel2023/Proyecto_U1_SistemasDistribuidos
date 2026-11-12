package ec.unl.votacion.votacion_service.exception;

public class HorarioEleccionInvalidoException extends EleccionDomainException {

    public HorarioEleccionInvalidoException() {
        super("La hora de fin debe ser posterior a la hora de inicio");
    }
}
