package es.nonomldev.gestiontunicas.importacion.dto;

import java.util.List;
import java.util.Map;

import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;
import es.nonomldev.gestiontunicas.importacion.entity.DecisionFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;

/** Fila del Excel en la pantalla de revisión. */
public record ImportacionFilaDTO(
		Long id,
		int numFila,
		/** nombreOriginal (columna 1 tal cual), apellidos, nombre, numeroHermano y fechaNacimiento (yyyy-MM-dd). */
		Map<String, Object> datos,
		EstadoFilaEnum estado,
		List<String> errores,
		/** Hermano existente con el que coincide, para compararlos lado a lado. */
		HermanoResumenDTO hermanoCoincidente,
		DecisionFilaEnum decision,
		Long entidadResultanteId) {
}
