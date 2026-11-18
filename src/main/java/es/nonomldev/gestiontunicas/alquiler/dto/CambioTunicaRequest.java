package es.nonomldev.gestiontunicas.alquiler.dto;

import java.util.List;

import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Diálogo "Cambiar túnica": devolución rápida de la actual (checklist de las 5 piezas)
 * y entrega de la nueva. El resguardo anterior se anula.
 */
public record CambioTunicaRequest(
		@NotNull Long tunicaNuevaId,
		@NotNull @Min(80) @Max(220) Integer alturaHermanoCm,
		@NotNull @Size(min = 5, max = 5) List<@Valid RevisionPiezaRequest> revisionTunicaActual,
		String observaciones) {
}
