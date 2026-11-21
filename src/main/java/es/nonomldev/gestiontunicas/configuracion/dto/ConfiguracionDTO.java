package es.nonomldev.gestiontunicas.configuracion.dto;

import es.nonomldev.gestiontunicas.configuracion.entity.TipoValorEnum;

public record ConfiguracionDTO(
		Long id,
		String clave,
		String valor,
		TipoValorEnum tipo,
		String descripcion) {
}
