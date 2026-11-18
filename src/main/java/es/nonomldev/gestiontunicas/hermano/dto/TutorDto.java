package es.nonomldev.gestiontunicas.hermano.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Datos del tutor. Se usa tanto en las respuestas como en las peticiones. */
public record TutorDto(
		@Size(max = 200) String nombre,
		@Size(max = 20) String telefono,
		@Email @Size(max = 150) String email) {
}
