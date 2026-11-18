package es.nonomldev.gestiontunicas.tunica.dto;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoPieza;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;

public record PiezaDto(
		Long id,
		TipoPieza tipo,
		EstadoPieza estado,
		String observaciones) {
}
