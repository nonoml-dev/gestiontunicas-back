package es.nonomldev.gestiontunicas.configuracion.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDTO;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.entity.ConfiguracionEntity;
import es.nonomldev.gestiontunicas.configuracion.entity.TipoValorEnum;
import es.nonomldev.gestiontunicas.configuracion.mapper.ConfiguracionMapper;
import es.nonomldev.gestiontunicas.configuracion.repository.ConfiguracionRepository;
import es.nonomldev.gestiontunicas.configuracion.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfiguracionServiceImpl implements ConfiguracionService {

	/**
	 * Límites de los enteros que, fuera de rango, dejarían la aplicación sin funcionar (0 intentos bloquearía
	 * cada inicio de sesión). La tolerancia usa los mismos límites que la del reparto.
	 */
	private static final Map<String, Rango> RANGOS = Map.of(
			"seguridad.intentos_maximos", new Rango(1, 20),
			"seguridad.minutos_bloqueo", new Rango(1, 1440),
			"reparto.tolerancia_medida_cm", new Rango(0, 30));

	private final ConfiguracionRepository repository;
	private final ConfiguracionMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<ConfiguracionDTO> listar(String prefijo) {
		List<ConfiguracionEntity> lista = prefijo == null || prefijo.isBlank()
				? repository.findAll(Sort.by("clave"))
				: repository.findByClaveStartingWithOrderByClave(prefijo);
		return mapper.toDTOs(lista);
	}

	@Override
	public ConfiguracionDTO actualizar(String clave, ConfiguracionRequest request) {
		ConfiguracionEntity configuracion = repository.findByClave(clave)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el parámetro de configuración " + clave));
		configuracion.setValor(normalizar(configuracion.getTipo(), request.valor(), clave));
		return mapper.toDTO(configuracion);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Integer> entero(String clave) {
		return repository.findByClave(clave).map(ConfiguracionEntity::comoEntero);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> texto(String clave) {
		return repository.findByClave(clave).map(ConfiguracionEntity::getValor);
	}

	/**
	 * Comprueba el valor según su tipo y lo deja limpio (sin espacios, booleanos en minúsculas). Un texto
	 * puede quedar vacío; el resto de tipos es obligatorio, porque quien los lee espera un valor.
	 */
	private static String normalizar(TipoValorEnum tipo, String valor, String clave) {
		if (valor == null || valor.isBlank()) {
			if (tipo == TipoValorEnum.TEXTO) {
				return null;
			}
			throw new ReglaNegocioException("El valor de %s es obligatorio".formatted(clave));
		}
		String limpio = valor.strip();
		try {
			switch (tipo) {
				case ENTERO -> comprobarRango(clave, Integer.parseInt(limpio));
				case DECIMAL -> new BigDecimal(limpio);
				case FECHA -> LocalDate.parse(limpio);
				case BOOLEANO -> {
					limpio = limpio.toLowerCase(Locale.ROOT);
					if (!limpio.equals("true") && !limpio.equals("false")) {
						throw new IllegalArgumentException();
					}
				}
				case TEXTO -> {
					// cualquier texto es válido
				}
			}
		} catch (IllegalArgumentException | DateTimeParseException e) {
			throw new ReglaNegocioException("El valor de %s no es un %s válido".formatted(clave, nombre(tipo)));
		}
		return limpio;
	}

	private static void comprobarRango(String clave, int valor) {
		Rango rango = RANGOS.get(clave);
		if (rango != null && (valor < rango.minimo() || valor > rango.maximo())) {
			throw new ReglaNegocioException("El valor de %s debe estar entre %d y %d"
					.formatted(clave, rango.minimo(), rango.maximo()));
		}
	}

	private static String nombre(TipoValorEnum tipo) {
		return switch (tipo) {
			case ENTERO -> "número entero";
			case DECIMAL -> "número";
			case FECHA -> "fecha (aaaa-mm-dd)";
			case BOOLEANO -> "sí o no (true o false)";
			case TEXTO -> "texto";
		};
	}

	private record Rango(int minimo, int maximo) {
	}

}
