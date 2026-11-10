package ec.unl.votacion.votacion_service.controller;

import ec.unl.votacion.votacion_service.dto.CrearVotoRequest;
import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.service.VotoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/votos")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping
    public ResponseEntity<VotoResponse> crearVoto(
            @Valid @RequestBody CrearVotoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(votoService.registrarVoto(request));
    }
}
