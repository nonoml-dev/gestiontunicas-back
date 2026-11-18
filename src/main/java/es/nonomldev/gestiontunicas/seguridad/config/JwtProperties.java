package es.nonomldev.gestiontunicas.seguridad.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de los tokens de sesión ({@code app.seguridad.jwt.*}).
 *
 * @param secret          clave HS256; al menos 32 caracteres. En producción se pasa con la variable de entorno JWT_SECRET.
 * @param duracion        validez del token de acceso (p. ej. PT15M).
 * @param duracionRefresh validez del refresh token (p. ej. PT12H): cuánto dura la sesión sin volver a entrar.
 */
@ConfigurationProperties("app.seguridad.jwt")
public record JwtProperties(String secret, Duration duracion, Duration duracionRefresh) {

	private static final int LONGITUD_MINIMA = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA) {
			throw new IllegalStateException(
					"app.seguridad.jwt.secret es obligatorio y debe tener al menos " + LONGITUD_MINIMA + " caracteres");
		}
		validarPositiva(duracion, "app.seguridad.jwt.duracion");
		validarPositiva(duracionRefresh, "app.seguridad.jwt.duracion-refresh");
	}

	private static void validarPositiva(Duration valor, String propiedad) {
		if (valor == null || valor.isNegative() || valor.isZero()) {
			throw new IllegalStateException(propiedad + " debe ser positiva");
		}
	}

}
