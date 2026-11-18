package es.nonomldev.gestiontunicas.devolucion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registrar la devolución con la revisión pieza a pieza. Fuera de plazo, gravedad y sanciones
 * los calcula el servicio.
 */
public record DevolucionRequest(
		@NotNull Long alquilerId,
		@NotNull @Size(min = 5, max = 5) List<@Valid RevisionPiezaRequest> revisiones,
		String observaciones) {
}
