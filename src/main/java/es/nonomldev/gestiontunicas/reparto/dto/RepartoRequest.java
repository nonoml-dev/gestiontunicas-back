package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Asistente de creación del reparto (y edición mientras está en BORRADOR).
 * La carga del censo y la plantilla del resguardo van en peticiones aparte.
 */
public record RepartoRequest(
		@NotNull @Min(2026) @Max(2100) Integer anio,
		/** Nulo = se calcula (Domingo de Resurrección − 3 días). Si viene, se marca como fijado a mano. */
		LocalDate fechaJuevesSanto,
		@NotNull @Min(1) @Max(25) Integer edadMin,
		@NotNull @Min(1) @Max(25) Integer edadMax,
		@NotNull @Valid PeriodoDevolucionRequest periodoDevolucion,
		@Size(max = 255) String lugarDevolucion,
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal limosna,
		@NotNull @Valid TarifaSancionesDto tarifas,
		@NotNull @Min(0) @Max(30) Integer toleranciaMedidaCm,
		boolean modoTransicion,
		/** Las 3 jornadas ordinarias, en orden. */
		@NotNull @Size(min = 3, max = 3) List<@Valid JornadaRequest> jornadas) {

	@AssertTrue(message = "la edad máxima no puede ser menor que la mínima")
	public boolean isEdadesCoherentes() {
		return edadMin == null || edadMax == null || edadMax >= edadMin;
	}

}
