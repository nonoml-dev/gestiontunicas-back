package es.nonomldev.gestiontunicas.configuracion.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.entity.ConfiguracionEntity;
import es.nonomldev.gestiontunicas.configuracion.entity.TipoValorEnum;
import es.nonomldev.gestiontunicas.configuracion.mapper.ConfiguracionMapperImpl;
import es.nonomldev.gestiontunicas.configuracion.repository.ConfiguracionRepository;

class ConfiguracionServiceImplTest {

	private static final String TOLERANCIA = "reparto.tolerancia_medida_cm";

	private final ConfiguracionRepository repository = mock(ConfiguracionRepository.class);
	private final ConfiguracionServiceImpl servicio = new ConfiguracionServiceImpl(repository, new ConfiguracionMapperImpl());

	@Test
	void cambiaLaToleranciaPorDefecto() {
		ConfiguracionEntity tolerancia = parametro(TOLERANCIA, "3", TipoValorEnum.ENTERO);

		var dto = servicio.actualizar(TOLERANCIA, new ConfiguracionRequest(" 5 "));

		assertThat(dto.valor()).isEqualTo("5");
		assertThat(tolerancia.getValor()).isEqualTo("5");
		assertThat(dto.clave()).isEqualTo(TOLERANCIA);
		assertThat(dto.tipo()).isEqualTo(TipoValorEnum.ENTERO);
	}

	@ParameterizedTest
	@CsvSource({ "reparto.tolerancia_medida_cm, -1", "reparto.tolerancia_medida_cm, 31",
			"seguridad.intentos_maximos, 0", "seguridad.intentos_maximos, 21",
			"seguridad.minutos_bloqueo, 0", "seguridad.minutos_bloqueo, 1441" })
	void rechazaLosEnterosFueraDeSuRango(String clave, String valor) {
		ConfiguracionEntity parametro = parametro(clave, "3", TipoValorEnum.ENTERO);

		assertThatThrownBy(() -> servicio.actualizar(clave, new ConfiguracionRequest(valor)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("debe estar entre");
		assertThat(parametro.getValor()).isEqualTo("3");
	}

	@ParameterizedTest
	@CsvSource({ "reparto.tolerancia_medida_cm, 0", "reparto.tolerancia_medida_cm, 30",
			"seguridad.intentos_maximos, 1", "seguridad.minutos_bloqueo, 1440" })
	void admiteLosLimitesDelRango(String clave, String valor) {
		parametro(clave, "3", TipoValorEnum.ENTERO);

		assertThat(servicio.actualizar(clave, new ConfiguracionRequest(valor)).valor()).isEqualTo(valor);
	}

	@ParameterizedTest
	@ValueSource(strings = { "tres", "3.5", "3 cm" })
	void unEnteroNoValidoSeRechaza(String valor) {
		parametro(TOLERANCIA, "3", TipoValorEnum.ENTERO);

		assertThatThrownBy(() -> servicio.actualizar(TOLERANCIA, new ConfiguracionRequest(valor)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("número entero");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void unValorQueNoEsTextoEsObligatorio(String valor) {
		parametro(TOLERANCIA, "3", TipoValorEnum.ENTERO);

		assertThatThrownBy(() -> servicio.actualizar(TOLERANCIA, new ConfiguracionRequest(valor)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("obligatorio");
	}

	@Test
	void unTextoPuedeQuedarVacio() {
		ConfiguracionEntity texto = parametro("smtp.remitente", "priostia@ejemplo.org", TipoValorEnum.TEXTO);

		servicio.actualizar("smtp.remitente", new ConfiguracionRequest(" "));

		assertThat(texto.getValor()).isNull();
	}

	@ParameterizedTest
	@CsvSource({ "TRUE, true", "False, false", "' true ', true" })
	void elEmailSeActivaYDesactivaConTrueOFalse(String valor, String guardado) {
		ConfiguracionEntity email = parametro("email.activado", "false", TipoValorEnum.BOOLEANO);

		servicio.actualizar("email.activado", new ConfiguracionRequest(valor));

		assertThat(email.getValor()).isEqualTo(guardado);
	}

	@ParameterizedTest
	@ValueSource(strings = { "si", "1", "activado" })
	void unBooleanoNoValidoSeRechaza(String valor) {
		parametro("email.activado", "false", TipoValorEnum.BOOLEANO);

		assertThatThrownBy(() -> servicio.actualizar("email.activado", new ConfiguracionRequest(valor)))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void unaClaveQueNoExisteDa404() {
		when(repository.findByClave("no.existe")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servicio.actualizar("no.existe", new ConfiguracionRequest("1")))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessageContaining("no.existe");
	}

	@Test
	void sinPrefijoListaTodoOrdenadoPorClave() {
		List<ConfiguracionEntity> todos = List.of(parametro("email.activado", "false", TipoValorEnum.BOOLEANO),
				parametro(TOLERANCIA, "3", TipoValorEnum.ENTERO));
		when(repository.findAll(Sort.by("clave"))).thenReturn(todos);

		assertThat(servicio.listar(null)).extracting("clave").containsExactly("email.activado", TOLERANCIA);
	}

	@Test
	void conPrefijoListaSoloEsaSeccion() {
		List<ConfiguracionEntity> seccion = List.of(parametro(TOLERANCIA, "3", TipoValorEnum.ENTERO));
		when(repository.findByClaveStartingWithOrderByClave("reparto.")).thenReturn(seccion);

		assertThat(servicio.listar("reparto.")).extracting("clave").containsExactly(TOLERANCIA);
	}

	private ConfiguracionEntity parametro(String clave, String valor, TipoValorEnum tipo) {
		ConfiguracionEntity parametro = new ConfiguracionEntity();
		parametro.setClave(clave);
		parametro.setValor(valor);
		parametro.setTipo(tipo);
		when(repository.findByClave(clave)).thenReturn(Optional.of(parametro));
		return parametro;
	}

}
