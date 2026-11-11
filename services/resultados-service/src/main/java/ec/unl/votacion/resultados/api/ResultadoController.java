package ec.unl.votacion.resultados.api;

import ec.unl.votacion.resultados.application.ResultadoQueryService;
import ec.unl.votacion.resultados.application.ResultadoSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final ResultadoQueryService queryService;

    public ResultadoController(ResultadoQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/elecciones/{eleccionId}")
    public ResultadoSnapshot obtener(@PathVariable UUID eleccionId) {
        return queryService.obtener(eleccionId);
    }
}
