package es.nonomldev.gestiontunicas.devolucion.dto;

import es.nonomldev.gestiontunicas.devolucion.entity.DefectoCapaEnum;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevisionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;

public record RevisionPiezaDTO(
		Long id,
		Long piezaId,
		TipoPiezaEnum tipoPieza,
		ResultadoRevisionEnum resultado,
		DefectoCapaEnum defectoCapa,
		String observaciones) {
}
