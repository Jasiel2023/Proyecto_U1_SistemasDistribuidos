package ec.unl.votacion.usuarios.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos invalidos", request, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restriccion(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(violacion -> violacion.getPropertyPath() + ": " + violacion.getMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La solicitud contiene datos invalidos", request, detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es valido", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violacion de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operacion viola una restriccion de integridad de datos", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error inesperado", request, List.of());
    }

    private ResponseEntity<ApiError> responder(HttpStatus status, String mensaje, HttpServletRequest request, List<String> detalles) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI(),
                detalles
        );
        return ResponseEntity.status(status).body(error);
    }
}