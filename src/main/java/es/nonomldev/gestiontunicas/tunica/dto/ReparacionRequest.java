package es.nonomldev.gestiontunicas.tunica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacion;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;
import es.nonomldev.gestiontunicas.tunica.entity.TipoReparacion;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Registrar o actualizar la reparación o reposición de una pieza de la túnica. */
public record ReparacionRequest(
		@NotNull TipoPieza tipoPieza,
		@NotNull TipoReparacion tipo,
		@NotBlank @Size(max = 500) String descripcion,
		/** Nulo = PENDIENTE. */
		EstadoReparacion estado,
		@NotNull LocalDate fechaInicio,
		LocalDate fechaFin,
		@PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal coste) {
}
