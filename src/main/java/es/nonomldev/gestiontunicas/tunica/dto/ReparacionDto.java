package es.nonomldev.gestiontunicas.tunica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacion;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;
import es.nonomldev.gestiontunicas.tunica.entity.TipoReparacion;

public record ReparacionDto(
		Long id,
		Long piezaId,
		TipoPieza tipoPieza,
		Long devolucionId,
		TipoReparacion tipo,
		String descripcion,
		EstadoReparacion estado,
		LocalDate fechaInicio,
		LocalDate fechaFin,
		BigDecimal coste) {
}
