package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;

public record ReservaDTO(
		Long id,
		Long repartoId,
		Long hermanoId,
		String hermanoNombreCompleto,
		Long tunicaId,
		String tunicaCodigo,
		EstadoReservaEnum estado,
		Long alquilerId,
		Instant resueltaEn) {
}
