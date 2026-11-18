package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.Instant;

public record CambioPeriodoDevolucionDto(
		Long id,
		PeriodoDevolucionDto anterior,
		PeriodoDevolucionDto nuevo,
		String motivo,
		String usuario,
		Instant fecha) {
}
