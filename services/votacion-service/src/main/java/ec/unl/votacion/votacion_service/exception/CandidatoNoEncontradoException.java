package ec.unl.votacion.votacion_service.exception;

public class CandidatoNoEncontradoException extends EleccionDomainException {

    public CandidatoNoEncontradoException(Long id, Long eleccionId) {
        super("No existe el candidato " + id + " para la eleccion " + eleccionId);
    }
}
