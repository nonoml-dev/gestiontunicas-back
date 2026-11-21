package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

/** Detalle del hermano: los datos del Excel de Secretaría más un email y dos teléfonos. */
public record HermanoDTO(
		Long id,
		String numeroHermano,
		String nombre,
		String apellidos,
		/** "Apellidos, Nombre", como en el Excel de Secretaría. */
		String nombreCompleto,
		LocalDate fechaNacimiento,
		String telefono1,
		String telefono2,
		String email,
		boolean activo,
		/** Sin fecha de nacimiento no puede alquilar. */
		boolean fichaCompleta) {
}
