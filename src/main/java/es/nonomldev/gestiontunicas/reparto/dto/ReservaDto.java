package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva;

public record ReservaDto(
		Long id,
		Long repartoId,
		Long hermanoId,
		String hermanoNombreCompleto,
		Long tunicaId,
		String tunicaCodigo,
		EstadoReserva estado,
		Long alquilerId,
		Instant resueltaEn) {
}
