package es.nonomldev.gestiontunicas.tunica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoReparacionEnum;

public record ReparacionDTO(
		Long id,
		Long piezaId,
		TipoPiezaEnum tipoPieza,
		Long devolucionId,
		TipoReparacionEnum tipo,
		String descripcion,
		EstadoReparacionEnum estado,
		LocalDate fechaInicio,
		LocalDate fechaFin,
		BigDecimal coste) {
}
