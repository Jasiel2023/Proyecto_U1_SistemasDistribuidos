package ec.unl.votacion.votacion_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EleccionAdminResponse(
        Long id,
        String titulo,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean estaActiva,
        List<CandidatoAdminResponse> candidatos
) {
}
