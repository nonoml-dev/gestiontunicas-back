package es.nonomldev.gestiontunicas.comun.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class VariablesProdEnvironmentPostProcessorTest {

	private final VariablesProdEnvironmentPostProcessor validador = new VariablesProdEnvironmentPostProcessor();

	@Test
	void arrancaEnProdConTodasLasVariables() {
		assertThatCode(() -> validador.postProcessEnvironment(prodCompleto(), null)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = { "DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET" })
	void noArrancaEnProdSiFaltaUnaVariable(String variable) {
		MockEnvironment entorno = new MockEnvironment();
		entorno.setActiveProfiles("prod");
		VariablesProdEnvironmentPostProcessor.OBLIGATORIAS.stream()
				.filter(nombre -> !nombre.equals(variable))
				.forEach(nombre -> entorno.setProperty(nombre, "valor"));

		assertThatThrownBy(() -> validador.postProcessEnvironment(entorno, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining(variable);
	}

	@Test
	void noArrancaEnProdSiUnaVariableEstaEnBlanco() {
		MockEnvironment entorno = prodCompleto();
		entorno.setProperty("DB_PASSWORD", "   ");

		assertThatThrownBy(() -> validador.postProcessEnvironment(entorno, null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("DB_PASSWORD");
	}

	@Test
	void noExigeNadaFueraDeProd() {
		MockEnvironment entorno = new MockEnvironment();
		entorno.setActiveProfiles("dev");

		assertThatCode(() -> validador.postProcessEnvironment(entorno, null)).doesNotThrowAnyException();
	}

	private static MockEnvironment prodCompleto() {
		MockEnvironment entorno = new MockEnvironment();
		entorno.setActiveProfiles("prod");
		VariablesProdEnvironmentPostProcessor.OBLIGATORIAS.forEach(nombre -> entorno.setProperty(nombre, "valor"));
		return entorno;
	}

}
