package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.Instant;

public record CambioPeriodoDevolucionDTO(
		Long id,
		PeriodoDevolucionDTO anterior,
		PeriodoDevolucionDTO nuevo,
		String motivo,
		String usuario,
		Instant fecha) {
}
