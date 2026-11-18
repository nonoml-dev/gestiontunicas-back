package es.nonomldev.gestiontunicas.tunica.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Alta o edición de una túnica. Al darla de alta se crean sus 5 piezas. */
public record TunicaRequest(
		@NotBlank
		@Pattern(regexp = "^([1-9][0-9]{0,2}|M-[0-9]{3})$", message = "debe ser un número del 1 al 999 o M-XXX")
		String codigo,
		@NotNull @Min(60) @Max(220) Integer medidaCm,
		@NotNull @Min(0) @Max(30) Integer dobladilloCm,
		/** Nula = hoy. */
		LocalDate fechaAlta,
		String observaciones) {
}
