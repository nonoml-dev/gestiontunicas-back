package es.nonomldev.gestiontunicas.tunica.dto;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;

/** Fila del inventario y túnica propuesta al entregar. */
public record TunicaResumenDto(
		Long id,
		String codigo,
		Integer medidaCm,
		Integer dobladilloCm,
		Integer medidaTotalCm,
		EstadoTunica estado) {
}
