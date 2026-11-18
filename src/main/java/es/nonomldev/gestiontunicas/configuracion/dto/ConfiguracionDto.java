package es.nonomldev.gestiontunicas.configuracion.dto;

import es.nonomldev.gestiontunicas.configuracion.entity.TipoValor;

public record ConfiguracionDto(
		Long id,
		String clave,
		String valor,
		TipoValor tipo,
		String descripcion) {
}
