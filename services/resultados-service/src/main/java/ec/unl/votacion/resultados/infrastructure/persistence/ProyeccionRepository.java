package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.application.RevisionActualizada;
import ec.unl.votacion.resultados.domain.VotoProcesado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProyeccionRepository {

    private static final String INSERT_VOTO = """
            INSERT INTO voto_procesado
                (event_id, voto_id, eleccion_id, candidato_id, occurred_at, procesado_en)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """;

    private static final String INCREMENTAR_RESUMEN = """
            INSERT INTO resultado_resumen
                (eleccion_id, total_votos, revision, ultima_actualizacion)
            VALUES (?, 1, 1, ?)
            ON CONFLICT (eleccion_id) DO UPDATE
                SET total_votos = resultado_resumen.total_votos + 1,
                    revision = resultado_resumen.revision + 1,
                    ultima_actualizacion = EXCLUDED.ultima_actualizacion
            RETURNING revision, total_votos, ultima_actualizacion
            """;

    private static final String INCREMENTAR_CONTEO = """
            INSERT INTO resultado_conteo
                (eleccion_id, candidato_id, votos, ultima_actualizacion)
            VALUES (?, ?, 1, ?)
            ON CONFLICT (eleccion_id, candidato_id) DO UPDATE
                SET votos = resultado_conteo.votos + 1,
                    ultima_actualizacion = EXCLUDED.ultima_actualizacion
            RETURNING votos
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public boolean insertarVoto(VotoProcesado voto) {
        int filas = entityManager.createNativeQuery(INSERT_VOTO)
                .setParameter(1, voto.getEventId())
                .setParameter(2, voto.getVotoId())
                .setParameter(3, voto.getEleccionId())
                .setParameter(4, voto.getCandidatoId())
                .setParameter(5, voto.getOccurredAt())
                .setParameter(6, voto.getProcesadoEn())
                .executeUpdate();
        return filas == 1;
    }

    public RevisionActualizada incrementarResumen(UUID eleccionId, OffsetDateTime instante) {
        Object[] fila = (Object[]) entityManager.createNativeQuery(INCREMENTAR_RESUMEN)
                .setParameter(1, eleccionId)
                .setParameter(2, instante)
                .getSingleResult();
        return new RevisionActualizada(
                ((Number) fila[0]).longValue(),
                ((Number) fila[1]).longValue(),
                aOffsetDateTime(fila[2]));
    }

    public long incrementarConteo(UUID eleccionId, UUID candidatoId, OffsetDateTime instante) {
        Object fila = entityManager.createNativeQuery(INCREMENTAR_CONTEO)
                .setParameter(1, eleccionId)
                .setParameter(2, candidatoId)
                .setParameter(3, instante)
                .getSingleResult();
        return ((Number) fila).longValue();
    }

    private static OffsetDateTime aOffsetDateTime(Object valor) {
        if (valor instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (valor instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (valor instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Tipo de timestamp no soportado: " + valor.getClass());
    }
}
