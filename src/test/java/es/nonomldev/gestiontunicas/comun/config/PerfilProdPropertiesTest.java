package es.nonomldev.gestiontunicas.comun.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerfilProdPropertiesTest {

	private final Properties prod = new Properties();

	@BeforeEach
	void cargar() throws IOException {
		try (InputStream fichero = getClass().getResourceAsStream("/application-prod.properties");
				Reader lector = new InputStreamReader(fichero, StandardCharsets.UTF_8)) {
			prod.load(lector);
		}
	}

	@Test
	void losSecretosNoTienenValorPorDefecto() {
		assertThat(prod.getProperty("spring.datasource.url")).isEqualTo("${DB_URL}");
		assertThat(prod.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
		assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
		assertThat(prod.getProperty("app.seguridad.jwt.secret")).isEqualTo("${JWT_SECRET}");
	}

	@Test
	void nuncaModificaElEsquemaNiExponeInformacionInterna() {
		assertThat(prod.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(prod.getProperty("spring.jpa.show-sql")).isEqualTo("false");
		assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
		assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
		assertThat(prod.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
		assertThat(prod.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
	}

}
