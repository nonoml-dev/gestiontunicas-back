package es.nonomldev.gestiontunicas.tunica.dto;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;

/** Fila del inventario y túnica propuesta al entregar. */
public record TunicaResumenDTO(
		Long id,
		String codigo,
		Integer medidaCm,
		Integer dobladilloCm,
		Integer medidaTotalCm,
		EstadoTunicaEnum estado) {
}
