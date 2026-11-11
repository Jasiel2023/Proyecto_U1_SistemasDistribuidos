package ec.unl.votacion.votacion_service.controller;

import ec.unl.votacion.votacion_service.dto.CandidatoAdminRequest;
import ec.unl.votacion.votacion_service.dto.CandidatoAdminResponse;
import ec.unl.votacion.votacion_service.dto.EleccionAdminRequest;
import ec.unl.votacion.votacion_service.dto.EleccionAdminResponse;
import ec.unl.votacion.votacion_service.service.EleccionAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/elecciones")
public class EleccionAdminController {

    private final EleccionAdminService adminService;

    public EleccionAdminController(EleccionAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<EleccionAdminResponse> listar() {
        return adminService.listar();
    }

    @PostMapping
    public ResponseEntity<EleccionAdminResponse> crear(@Valid @RequestBody EleccionAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.crear(request));
    }

    @PutMapping("/{id}")
    public EleccionAdminResponse actualizar(@PathVariable UUID id,
                                            @Valid @RequestBody EleccionAdminRequest request) {
        return adminService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        adminService.eliminar(id);
    }

    @PostMapping("/{eleccionId}/candidatos")
    public ResponseEntity<CandidatoAdminResponse> agregarCandidato(
            @PathVariable UUID eleccionId,
            @Valid @RequestBody CandidatoAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.agregarCandidato(eleccionId, request));
    }

    @DeleteMapping("/{eleccionId}/candidatos/{candidatoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCandidato(@PathVariable UUID eleccionId, @PathVariable UUID candidatoId) {
        adminService.eliminarCandidato(eleccionId, candidatoId);
    }
}
