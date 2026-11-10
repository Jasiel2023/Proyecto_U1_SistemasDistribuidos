package ec.unl.votacion.votacion_service.exception;

public class CandidatoInactivoException extends EleccionDomainException {

    public CandidatoInactivoException(Long id) {
        super("El candidato " + id + " no esta activo");
    }
}
