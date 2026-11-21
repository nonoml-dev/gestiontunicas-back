package es.nonomldev.gestiontunicas.comun.config;

import java.util.List;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * En el perfil prod impide arrancar si falta un secreto.
 * <p>
 * Es necesario porque Spring deja sin resolver un {@code ${VARIABLE}} ausente y lo usaría como valor literal:
 * por ejemplo, la contraseña de la base de datos sería el texto {@code ${DB_PASSWORD}}.
 */
public class VariablesProdEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final List<String> OBLIGATORIAS = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET");

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!environment.acceptsProfiles(Profiles.of("prod"))) {
			return;
		}
		List<String> faltan = OBLIGATORIAS.stream()
				.filter(variable -> {
					String valor = environment.getProperty(variable);
					return valor == null || valor.isBlank();
				})
				.toList();
		if (!faltan.isEmpty()) {
			throw new IllegalStateException("Faltan variables de entorno obligatorias en producción: " + faltan);
		}
	}

}
