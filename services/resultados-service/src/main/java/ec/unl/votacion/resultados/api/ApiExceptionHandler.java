package ec.unl.votacion.resultados.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail solicitudInvalida(MethodArgumentTypeMismatchException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "El identificador de eleccion no es un numero valido.");
        detail.setTitle("Solicitud invalida");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail errorInterno(Exception ex) {
        log.error("Error no controlado en la API de resultados", ex);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "No fue posible procesar la consulta de resultados.");
        detail.setTitle("Error interno");
        return detail;
    }
}
