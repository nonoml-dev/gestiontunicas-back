package es.nonomldev.gestiontunicas.seguridad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales del primer administrador ({@code app.seguridad.admin.*}).
 * Se pasan con las variables de entorno ADMIN_USERNAME y ADMIN_PASSWORD.
 *
 * @param username nombre de usuario.
 * @param password contraseña inicial; hay que cambiarla en el primer acceso.
 */
@ConfigurationProperties("app.seguridad.admin")
public record AdminInicialProperties(String username, String password) {

	public boolean definidas() {
		return username != null && !username.isBlank() && password != null && !password.isBlank();
	}

}
