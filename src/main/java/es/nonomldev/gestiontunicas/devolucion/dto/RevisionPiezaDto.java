package es.nonomldev.gestiontunicas.devolucion.dto;

import es.nonomldev.gestiontunicas.devolucion.entity.DefectoCapa;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevision;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;

public record RevisionPiezaDto(
		Long id,
		Long piezaId,
		TipoPieza tipoPieza,
		ResultadoRevision resultado,
		DefectoCapa defectoCapa,
		String observaciones) {
}
