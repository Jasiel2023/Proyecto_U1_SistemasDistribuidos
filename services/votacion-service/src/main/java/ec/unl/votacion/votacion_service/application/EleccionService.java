package ec.unl.votacion.votacion_service.application;

import ec.unl.votacion.votacion_service.api.CrearEleccionRequest;
import ec.unl.votacion.votacion_service.api.CrearCandidatoRequest;
import ec.unl.votacion.votacion_service.api.EleccionResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EleccionService {

    private final JdbcTemplate jdbcTemplate;

    public EleccionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public EleccionResponse crear(CrearEleccionRequest request) {
        UUID id = UUID.randomUUID();
        OffsetDateTime inicio = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update("INSERT INTO elecciones (id, titulo, descripcion, fecha, hora_inicio, esta_activa) "
                        + "VALUES (?, ?, ?, ?, ?, TRUE)",
                id, request.titulo().trim(), request.descripcion() == null ? "" : request.descripcion().trim(),
                LocalDate.now(ZoneOffset.UTC), LocalTime.now(ZoneOffset.UTC));
        for (CrearCandidatoRequest candidato : request.candidatos()) {
            jdbcTemplate.update("INSERT INTO candidatos (id, eleccion_id, nombre, apellido, partido_politico) VALUES (?, ?, ?, '', ?)",
                    UUID.randomUUID(), id, candidato.nombre().trim(), candidato.partido() == null ? "" : candidato.partido().trim());
        }
        return obtener(id);
    }

    @Transactional(readOnly = true)
    public List<EleccionResponse> listarAbiertas() {
        List<UUID> ids = jdbcTemplate.query("SELECT id FROM elecciones WHERE esta_activa = TRUE ORDER BY fecha DESC, hora_inicio DESC",
                (rs, row) -> rs.getObject("id", UUID.class));
        return ids.stream().map(this::obtener).toList();
    }

    @Transactional(readOnly = true)
    public EleccionResponse obtener(UUID id) {
        List<EleccionResponse> filas = jdbcTemplate.query(
                "SELECT id, titulo, descripcion, fecha, hora_inicio, hora_fin, esta_activa FROM elecciones WHERE id = ?",
                (rs, row) -> mapEleccion(rs), id);
        if (filas.isEmpty()) {
            throw new EleccionNoEncontradaException("No existe la eleccion " + id);
        }
        EleccionResponse e = filas.get(0);
        List<EleccionResponse.CandidatoResponse> candidatos = jdbcTemplate.query(
                "SELECT id, nombre, partido_politico FROM candidatos WHERE eleccion_id = ? ORDER BY nombre",
                (rs, row) -> new EleccionResponse.CandidatoResponse(
                        rs.getObject("id", UUID.class), rs.getString("nombre"), rs.getString("partido_politico")), id);
        return new EleccionResponse(e.id(), e.titulo(), e.descripcion(), e.estado(), e.fechaInicio(), e.fechaFin(), candidatos);
    }

    private EleccionResponse mapEleccion(ResultSet rs) throws SQLException {
        LocalDate fecha = rs.getObject("fecha", LocalDate.class);
        LocalTime inicio = rs.getObject("hora_inicio", LocalTime.class);
        LocalTime fin = rs.getObject("hora_fin", LocalTime.class);
        OffsetDateTime inicioUtc = fecha.atTime(inicio).atOffset(ZoneOffset.UTC);
        OffsetDateTime finUtc = fin == null ? null : fecha.atTime(fin).atOffset(ZoneOffset.UTC);
        return new EleccionResponse(rs.getObject("id", UUID.class), rs.getString("titulo"),
                rs.getString("descripcion"), rs.getBoolean("esta_activa") ? "OPEN" : "CLOSED",
                inicioUtc, finUtc, List.of());
    }
}
