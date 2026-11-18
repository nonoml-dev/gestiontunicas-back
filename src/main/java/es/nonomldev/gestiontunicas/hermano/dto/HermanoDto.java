package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

/** Ficha del hermano. */
public record HermanoDto(
		Long id,
		String numeroHermano,
		String nombre,
		String apellidos,
		/** "Apellidos, Nombre", como en el Excel de Secretaría. */
		String nombreCompleto,
		LocalDate fechaNacimiento,
		String telefono,
		String email,
		String direccion,
		String localidad,
		String codigoPostal,
		TutorDto tutor,
		/** Teléfonos del hermano y del tutor separados por " / ". */
		String telefonos,
		boolean aceptaEmail,
		boolean consentimientoRgpd,
		LocalDate fechaConsentimiento,
		boolean activo,
		boolean fichaCompleta,
		String observaciones) {
}
