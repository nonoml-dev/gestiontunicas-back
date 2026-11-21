package es.nonomldev.gestiontunicas.seguridad.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Gestión de usuarios: solo el Administrador. No se borran: se desactivan con {@code activo=false}
 * para conservar quién hizo cada operación.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

	private final UsuarioService usuarioService;

	@GetMapping
	public List<UsuarioDTO> listar() {
		return usuarioService.listar();
	}

	@GetMapping("/{id}")
	public UsuarioDTO obtener(@PathVariable Long id) {
		return usuarioService.obtener(id);
	}

	/** Devuelve la contraseña temporal: solo se muestra en esta respuesta. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioCreadoDTO crear(@Valid @RequestBody UsuarioRequest request) {
		return usuarioService.crear(request);
	}

	@PutMapping("/{id}")
	public UsuarioDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
		return usuarioService.actualizar(id, request);
	}

	/** Genera una contraseña temporal nueva, desbloquea al usuario y cierra sus sesiones. */
	@PostMapping("/{id}/restablecer-password")
	public UsuarioCreadoDTO restablecerPassword(@PathVariable Long id) {
		return usuarioService.restablecerPassword(id);
	}

}
