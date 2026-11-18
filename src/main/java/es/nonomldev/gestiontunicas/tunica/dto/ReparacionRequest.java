package es.nonomldev.gestiontunicas.tunica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoReparacionEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Registrar o actualizar la reparación o reposición de una pieza de la túnica. */
public record ReparacionRequest(
		@NotNull TipoPiezaEnum tipoPieza,
		@NotNull TipoReparacionEnum tipo,
		@NotBlank @Size(max = 500) String descripcion,
		/** Nulo = PENDIENTE. */
		EstadoReparacionEnum estado,
		@NotNull LocalDate fechaInicio,
		LocalDate fechaFin,
		@PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal coste) {
}
