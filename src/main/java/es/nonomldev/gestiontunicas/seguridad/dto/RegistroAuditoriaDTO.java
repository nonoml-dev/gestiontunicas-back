package es.nonomldev.gestiontunicas.seguridad.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;

/** Un inicio de sesión del registro (pantalla del Administrador). */
public record RegistroAuditoriaDTO(
		Long id,
		Instant fecha,
		Long usuarioId,
		String username,
		ResultadoAccesoEnum resultado,
		String ip) {
}
