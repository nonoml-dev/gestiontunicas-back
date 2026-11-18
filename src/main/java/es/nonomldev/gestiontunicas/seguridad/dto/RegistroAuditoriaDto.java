package es.nonomldev.gestiontunicas.seguridad.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAcceso;

/** Un inicio de sesión del registro (pantalla del Administrador). */
public record RegistroAuditoriaDto(
		Long id,
		Instant fecha,
		Long usuarioId,
		String username,
		ResultadoAcceso resultado,
		String ip) {
}
