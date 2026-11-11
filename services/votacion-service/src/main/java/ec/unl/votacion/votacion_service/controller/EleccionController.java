package ec.unl.votacion.votacion_service.controller;

import ec.unl.votacion.votacion_service.dto.EleccionResponse;
import ec.unl.votacion.votacion_service.service.VotoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/elecciones")
public class EleccionController {

    private final VotoService votoService;

    public EleccionController(VotoService votoService) {
        this.votoService = votoService;
    }

    @GetMapping
    public List<EleccionResponse> listarElecciones() {
        return votoService.listarEleccionesActivas();
    }
}
