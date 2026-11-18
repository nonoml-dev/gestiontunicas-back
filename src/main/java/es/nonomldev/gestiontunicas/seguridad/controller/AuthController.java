package es.nonomldev.gestiontunicas.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.seguridad.dto.LoginDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.RefreshTokenRequest;
import es.nonomldev.gestiontunicas.seguridad.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Inicio, renovación y cierre de sesión. Son los únicos endpoints públicos de la API.
 * Si se rechaza, responde 401 con el motivo en {@code detail} (ver {@code ManejadorErrores}).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public LoginDTO login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		return authService.login(request, httpRequest.getRemoteAddr());
	}

	@PostMapping("/refresh")
	public LoginDTO refrescar(@Valid @RequestBody RefreshTokenRequest request) {
		return authService.refrescar(request.refreshToken());
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cerrarSesion(@Valid @RequestBody RefreshTokenRequest request) {
		authService.cerrarSesion(request.refreshToken());
	}

}
