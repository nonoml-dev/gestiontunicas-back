package es.nonomldev.gestiontunicas.sancion.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.sancion.entity.OrigenVetoEnum;

public record VetoDTO(
		Long id,
		Long hermanoId,
		String hermanoNombreCompleto,
		OrigenVetoEnum origen,
		/** Año del reparto en el que no puede alquilar (el veto dura un solo año). */
		Integer anioVetado,
		Long alquilerId,
		String motivo,
		Instant fecha,
		String usuario,
		boolean activo,
		Instant fechaLevantamiento,
		String usuarioLevantamiento,
		String motivoLevantamiento) {
}
