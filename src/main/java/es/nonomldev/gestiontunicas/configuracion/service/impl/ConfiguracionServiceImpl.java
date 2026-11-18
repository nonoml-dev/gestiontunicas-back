package es.nonomldev.gestiontunicas.configuracion.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDto;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.entity.Configuracion;
import es.nonomldev.gestiontunicas.configuracion.entity.TipoValor;
import es.nonomldev.gestiontunicas.configuracion.mapper.ConfiguracionMapper;
import es.nonomldev.gestiontunicas.configuracion.repository.ConfiguracionRepository;
import es.nonomldev.gestiontunicas.configuracion.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfiguracionServiceImpl implements ConfiguracionService {

	private final ConfiguracionRepository repository;
	private final ConfiguracionMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<ConfiguracionDto> listar(String prefijo) {
		List<Configuracion> lista = prefijo == null || prefijo.isBlank()
				? repository.findAll()
				: repository.findByClaveStartingWithOrderByClave(prefijo);
		return mapper.toDtos(lista);
	}

	@Override
	public ConfiguracionDto actualizar(String clave, ConfiguracionRequest request) {
		Configuracion configuracion = repository.findByClave(clave)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el parámetro de configuración " + clave));
		validar(configuracion.getTipo(), request.valor(), clave);
		mapper.actualizar(request, configuracion);
		return mapper.toDto(configuracion);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Integer> entero(String clave) {
		return repository.findByClave(clave).map(Configuracion::comoEntero);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> texto(String clave) {
		return repository.findByClave(clave).map(Configuracion::getValor);
	}

	private static void validar(TipoValor tipo, String valor, String clave) {
		if (valor == null || valor.isBlank()) {
			return;
		}
		try {
			switch (tipo) {
				case ENTERO -> Integer.parseInt(valor.trim());
				case DECIMAL -> new BigDecimal(valor.trim());
				case FECHA -> LocalDate.parse(valor.trim());
				case BOOLEANO -> {
					if (!valor.trim().equalsIgnoreCase("true") && !valor.trim().equalsIgnoreCase("false")) {
						throw new IllegalArgumentException();
					}
				}
				case TEXTO -> {
					// cualquier texto es válido
				}
			}
		} catch (IllegalArgumentException | DateTimeParseException e) {
			throw new ReglaNegocioException("El valor de %s no es un %s válido".formatted(clave, tipo.name().toLowerCase()));
		}
	}

}
