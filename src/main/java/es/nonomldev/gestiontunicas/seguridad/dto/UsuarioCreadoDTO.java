package es.nonomldev.gestiontunicas.seguridad.dto;

/**
 * Respuesta al crear un usuario o restablecer su contraseña: la contraseña temporal solo se
 * muestra esta vez y el usuario tendrá que cambiarla en su primer acceso.
 */
public record UsuarioCreadoDTO(
		UsuarioDTO usuario,
		String passwordTemporal) {
}
