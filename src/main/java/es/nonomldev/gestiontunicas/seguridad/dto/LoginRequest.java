package es.nonomldev.gestiontunicas.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de la pantalla de inicio de sesión. */
public record LoginRequest(
		@NotBlank @Size(max = 50) String username,
		@NotBlank @Size(max = 100) String password) {
}
