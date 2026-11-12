package ec.unl.votacion.votacion_service.api;

import ec.unl.votacion.votacion_service.application.VotacionService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votos")
public class VotoController {

    private final VotacionService votacionService;

    public VotoController(VotacionService votacionService) {
        this.votacionService = votacionService;
    }

    @PostMapping
    public ResponseEntity<VotoResponse> emitir(@Valid @RequestBody EmitirVotoRequest request) {
        VotoResponse response = votacionService.emitir(request);
        return ResponseEntity.created(URI.create("/api/votos/" + response.votoId())).body(response);
    }
}
