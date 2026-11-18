package es.nonomldev.gestiontunicas.hermano.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Edición de un hermano: solo los datos de contacto. Los datos del hermano (nº, apellidos, nombre y fecha de
 * nacimiento) vienen del Excel de Secretaría y no se editan.
 */
public record ContactoHermanoRequest(
		@Size(max = 20) String telefono1,
		@Size(max = 20) String telefono2,
		@Email @Size(max = 150) String email) {
}
