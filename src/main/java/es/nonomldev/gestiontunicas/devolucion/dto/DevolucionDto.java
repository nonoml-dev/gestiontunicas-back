package es.nonomldev.gestiontunicas.devolucion.dto;

import java.time.Instant;
import java.util.List;

import es.nonomldev.gestiontunicas.devolucion.entity.Gravedad;

public record DevolucionDto(
		Long id,
		Long alquilerId,
		Instant fecha,
		String usuario,
		boolean porCambio,
		boolean fueraPlazo,
		Integer diasRetraso,
		Gravedad gravedad,
		String observaciones,
		List<RevisionPiezaDto> revisiones) {
}
