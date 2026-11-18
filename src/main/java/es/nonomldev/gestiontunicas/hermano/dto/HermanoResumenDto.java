package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

/** Fila del listado y del buscador de hermanos. */
public record HermanoResumenDto(
		Long id,
		String nombreCompleto,
		LocalDate fechaNacimiento,
		String tutorNombre,
		boolean fichaCompleta) {
}
