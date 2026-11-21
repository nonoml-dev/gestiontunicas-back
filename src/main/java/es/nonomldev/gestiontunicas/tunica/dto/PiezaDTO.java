package es.nonomldev.gestiontunicas.tunica.dto;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;

public record PiezaDTO(
		Long id,
		TipoPiezaEnum tipo,
		EstadoPiezaEnum estado,
		String observaciones) {
}
