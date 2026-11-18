package es.nonomldev.gestiontunicas.devolucion.dto;

import es.nonomldev.gestiontunicas.devolucion.entity.DefectoCapaEnum;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevisionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Una fila del checklist de las 5 piezas. */
public record RevisionPiezaRequest(
		@NotNull TipoPiezaEnum tipoPieza,
		@NotNull ResultadoRevisionEnum resultado,
		/** Solo en la capa: escudo JHS despegado o perdido (cuenta como deteriorada). */
		DefectoCapaEnum defectoCapa,
		String observaciones) {

	@AssertTrue(message = "el defecto del escudo JHS solo se puede marcar en la capa")
	public boolean isDefectoSoloEnCapa() {
		return defectoCapa == null || tipoPieza == TipoPiezaEnum.CAPA;
	}

}
