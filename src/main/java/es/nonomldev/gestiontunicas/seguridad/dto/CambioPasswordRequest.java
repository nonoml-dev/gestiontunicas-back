package es.nonomldev.gestiontunicas.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña del propio usuario. BCrypt admite como máximo 72 bytes. */
public record CambioPasswordRequest(
		@NotBlank String passwordActual,
		@NotBlank @Size(min = 8, max = 72) String passwordNueva) {
}
