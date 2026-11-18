package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.LocalDate;

/** Periodo de devolución con el fin del plazo de gracia ya calculado. */
public record PeriodoDevolucionDto(
		LocalDate inicio,
		LocalDate fin,
		Integer plazoGraciaMeses,
		LocalDate finPlazoGracia) {
}
