package es.nonomldev.gestiontunicas.devolucion.dto;

import es.nonomldev.gestiontunicas.devolucion.entity.DefectoCapa;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevision;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Una fila del checklist de las 5 piezas. */
public record RevisionPiezaRequest(
		@NotNull TipoPieza tipoPieza,
		@NotNull ResultadoRevision resultado,
		/** Solo en la capa: escudo JHS despegado o perdido (cuenta como deteriorada). */
		DefectoCapa defectoCapa,
		String observaciones) {

	@AssertTrue(message = "el defecto del escudo JHS solo se puede marcar en la capa")
	public boolean isDefectoSoloEnCapa() {
		return defectoCapa == null || tipoPieza == TipoPieza.CAPA;
	}

}
