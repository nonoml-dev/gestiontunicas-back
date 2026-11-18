package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición manual de un hermano (Secretaría).
 * La fecha de nacimiento puede faltar: el hermano queda con la ficha incompleta y no puede alquilar.
 */
public record HermanoRequest(
		@Size(max = 10) String numeroHermano,
		@NotBlank @Size(max = 100) String nombre,
		@NotBlank @Size(max = 150) String apellidos,
		@Past LocalDate fechaNacimiento,
		@Size(max = 20) String telefono,
		@Email @Size(max = 150) String email,
		@Size(max = 255) String direccion,
		@Size(max = 100) String localidad,
		@Size(max = 10) String codigoPostal,
		@Valid TutorDto tutor,
		boolean aceptaEmail,
		boolean consentimientoRgpd,
		String observaciones) {
}
