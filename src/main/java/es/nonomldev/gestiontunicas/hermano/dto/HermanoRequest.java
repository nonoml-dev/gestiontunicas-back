package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Alta o edición de un hermano: los datos del Excel más un email y dos teléfonos. */
public record HermanoRequest(
		@Size(max = 10) String numeroHermano,
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(max = 150) String apellidos,
		@Past LocalDate fechaNacimiento,
		@Size(max = 20) String telefono1,
		@Size(max = 20) String telefono2,
		@Email @Size(max = 150) String email) {
}
