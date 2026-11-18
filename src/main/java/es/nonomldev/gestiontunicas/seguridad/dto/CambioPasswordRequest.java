package es.nonomldev.gestiontunicas.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña del propio usuario. */
public record CambioPasswordRequest(
		@NotBlank String passwordActual,
		@NotBlank @Size(min = 8, max = 100) String passwordNueva) {
}
