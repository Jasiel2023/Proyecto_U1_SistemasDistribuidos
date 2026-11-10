package ec.unl.votacion.votacion_service.exception;

import ec.unl.votacion.votacion_service.dto.ErrorResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::mensajeDeCampo)
                .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(VotacionDomainException.class)
    public ResponseEntity<ErrorResponse> manejarDominio(
            VotacionDomainException exception
    ) {
        HttpStatus status = switch (exception) {
            case VotacionNoEncontradaException ignored -> HttpStatus.NOT_FOUND;
            case OpcionNoEncontradaException ignored -> HttpStatus.NOT_FOUND;
            case VotacionCerradaException ignored -> HttpStatus.CONFLICT;
            case OpcionInactivaException ignored -> HttpStatus.CONFLICT;
            case VotoDuplicadoException ignored -> HttpStatus.CONFLICT;
            default -> HttpStatus.CONFLICT;
        };

        return error(status, exception.getClass().getSimpleName(), exception.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> manejarBaseDeDatos(
            DataAccessException exception
    ) {
        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "DATABASE_UNAVAILABLE",
                "No fue posible completar la operacion"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarErrorInterno(Exception exception) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrio un error interno"
        );
    }

    private String mensajeDeCampo(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), code, message, Instant.now()));
    }
}
