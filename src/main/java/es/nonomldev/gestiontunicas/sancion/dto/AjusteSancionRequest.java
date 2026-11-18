package es.nonomldev.gestiontunicas.sancion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Ajustar el importe calculado de una sanción. El motivo es obligatorio. */
public record AjusteSancionRequest(
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importe,
		@NotBlank @Size(max = 255) String motivo) {
}
