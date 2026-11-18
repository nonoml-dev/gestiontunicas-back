package es.nonomldev.gestiontunicas.importacion.dto;

import java.util.List;
import java.util.Map;

import es.nonomldev.gestiontunicas.importacion.entity.DecisionFila;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFila;

/** Fila del Excel en la pantalla de revisión. */
public record ImportacionFilaDto(
		Long id,
		int numFila,
		Map<String, Object> datos,
		EstadoFila estado,
		List<String> errores,
		Long hermanoCoincidenteId,
		DecisionFila decision,
		Long entidadResultanteId) {
}
