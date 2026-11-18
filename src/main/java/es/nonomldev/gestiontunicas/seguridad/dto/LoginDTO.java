package es.nonomldev.gestiontunicas.seguridad.dto;

import java.time.Instant;

/**
 * Respuesta de un inicio de sesión o de una renovación de la sesión.
 *
 * @param accessToken      JWT que se envía en {@code Authorization: Bearer}; dura poco.
 * @param expiraEn         caducidad del token de acceso.
 * @param refreshToken     token opaco para pedir un nuevo token de acceso; se cambia en cada renovación.
 * @param refreshExpiraEn  caducidad del refresh token: a partir de ahí hay que volver a entrar.
 */
public record LoginDTO(
		String accessToken,
		String tipo,
		Instant expiraEn,
		String refreshToken,
		Instant refreshExpiraEn,
		UsuarioDTO usuario) {
}
