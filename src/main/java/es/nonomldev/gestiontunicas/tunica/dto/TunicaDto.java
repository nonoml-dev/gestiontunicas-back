package es.nonomldev.gestiontunicas.tunica.dto;

import java.time.LocalDate;
import java.util.List;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;

/** Ficha de la túnica con sus 5 piezas. */
public record TunicaDto(
		Long id,
		String codigo,
		boolean monaguillo,
		Integer medidaCm,
		Integer dobladilloCm,
		Integer medidaTotalCm,
		EstadoTunica estado,
		LocalDate fechaAlta,
		LocalDate fechaBaja,
		String motivoBaja,
		LocalDate fechaPropiedad,
		String observaciones,
		List<PiezaDto> piezas) {
}
