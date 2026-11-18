package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PeriodoDevolucionRequest(
		@NotNull LocalDate inicio,
		@NotNull LocalDate fin,
		/** Meses para devolverla tras la fecha fin. Por defecto 2. */
		@NotNull @Min(0) @Max(12) Integer plazoGraciaMeses) {

	@AssertTrue(message = "la fecha fin no puede ser anterior a la de inicio")
	public boolean isFechasCoherentes() {
		return inicio == null || fin == null || !fin.isBefore(inicio);
	}

}
