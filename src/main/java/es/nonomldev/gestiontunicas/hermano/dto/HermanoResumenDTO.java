package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

/** Fila del listado de hermanos: solo los datos que vienen en el Excel de Secretaría. */
public record HermanoResumenDTO(
		Long id,
		String numeroHermano,
		/** "Apellidos, Nombre". */
		String nombreCompleto,
		LocalDate fechaNacimiento,
		boolean fichaCompleta) {
}
