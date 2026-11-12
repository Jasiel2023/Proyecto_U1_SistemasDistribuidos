package ec.unl.votacion.votacion_service.api;

import ec.unl.votacion.votacion_service.application.EleccionNoEncontradaException;
import ec.unl.votacion.votacion_service.application.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.application.VotoInvalidoException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(VotoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> votoDuplicado(VotoDuplicadoException ex) {
        return error(HttpStatus.CONFLICT, "VOTO_DUPLICADO", ex.getMessage());
    }

    @ExceptionHandler({EleccionNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> noEncontrado(RuntimeException ex) {
        return error(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage());
    }

    @ExceptionHandler(VotoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> votoInvalido(VotoInvalidoException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VOTO_INVALIDO", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", OffsetDateTime.now(ZoneOffset.UTC),
                "status", status.value(),
                "codigo", codigo,
                "mensaje", mensaje));
    }
}
