package ec.unl.votacion.resultados.application;

import ec.unl.votacion.resultados.domain.Resumen;
import ec.unl.votacion.resultados.infrastructure.persistence.ConteoCandidatoRepository;
import ec.unl.votacion.resultados.infrastructure.persistence.ResumenRepository;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoQueryService {

    private final ResumenRepository resumenRepository;
    private final ConteoCandidatoRepository conteoRepository;

    public ResultadoQueryService(ResumenRepository resumenRepository,
                                 ConteoCandidatoRepository conteoRepository) {
        this.resumenRepository = resumenRepository;
        this.conteoRepository = conteoRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ,
            propagation = Propagation.REQUIRES_NEW)
    public ResultadoSnapshot obtener(UUID eleccionId) {
        Optional<Resumen> resumen = resumenRepository.findById(eleccionId);
        if (resumen.isEmpty()) {
            return ResultadoSnapshot.vacio(eleccionId);
        }
        var conteos = conteoRepository.findByIdEleccionIdOrderByIdCandidatoIdAsc(eleccionId).stream()
                .map(conteo -> new ResultadoSnapshot.ConteoSnapshot(
                        conteo.getId().getCandidatoId(), conteo.getVotos()))
                .toList();
        Resumen r = resumen.get();
        return new ResultadoSnapshot(
                eleccionId,
                r.getTotalVotos(),
                conteos,
                r.getRevision(),
                r.getUltimaActualizacion().withOffsetSameInstant(ZoneOffset.UTC),
                true);
    }
}
