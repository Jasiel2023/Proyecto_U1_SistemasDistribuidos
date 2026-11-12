package ec.unl.votacion.votacion_service.api;

import ec.unl.votacion.votacion_service.application.EleccionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/elecciones")
public class EleccionController {

    private final EleccionService eleccionService;

    public EleccionController(EleccionService eleccionService) {
        this.eleccionService = eleccionService;
    }

    @PostMapping
    public ResponseEntity<EleccionResponse> crear(@Valid @RequestBody CrearEleccionRequest request) {
        EleccionResponse creada = eleccionService.crear(request);
        return ResponseEntity.created(URI.create("/api/elecciones/" + creada.id())).body(creada);
    }

    @GetMapping
    public List<EleccionResponse> catalogo() {
        return eleccionService.listarAbiertas();
    }

    @GetMapping("/{id}")
    public EleccionResponse obtener(@PathVariable UUID id) {
        return eleccionService.obtener(id);
    }
}
