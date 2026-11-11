package ec.unl.votacion.votacion_service.dto;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record EleccionResponse(
        UUID id,
        String titulo,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        List<CandidatoResponse> candidatos
) {
}
