package ec.unl.votacion.usuarios.controller;

import ec.unl.votacion.usuarios.dto.CrearUsuarioRequest;
import ec.unl.votacion.usuarios.dto.UsuarioResponse;
import ec.unl.votacion.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse response = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Integer id) {
        UsuarioResponse response = usuarioService.obtenerPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cuenta/{idCuenta}")
    public ResponseEntity<UsuarioResponse> obtenerPorIdCuenta(@PathVariable Integer idCuenta) {
        UsuarioResponse response = usuarioService.obtenerPorIdCuenta(idCuenta);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<Boolean> existePorCedula(@PathVariable String cedula) {
        boolean existe = usuarioService.existePorCedula(cedula);
        return ResponseEntity.ok(existe);
    }

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort
    ) {
        int tamano = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(Math.max(page, 0), tamano, Sort.by(sort));
        Page<UsuarioResponse> response = usuarioService.listar(pageable);
        return ResponseEntity.ok(response);
    }
}