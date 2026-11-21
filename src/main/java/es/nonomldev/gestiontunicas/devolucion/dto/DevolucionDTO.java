package es.nonomldev.gestiontunicas.devolucion.dto;

import java.time.Instant;
import java.util.List;

import es.nonomldev.gestiontunicas.devolucion.entity.GravedadEnum;

public record DevolucionDTO(
		Long id,
		Long alquilerId,
		Instant fecha,
		String usuario,
		boolean porCambio,
		boolean fueraPlazo,
		Integer diasRetraso,
		GravedadEnum gravedad,
		String observaciones,
		List<RevisionPiezaDTO> revisiones) {
}
