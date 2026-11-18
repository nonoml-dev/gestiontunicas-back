package es.nonomldev.gestiontunicas.configuracion.service;

import java.util.List;
import java.util.Optional;

import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDto;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;

/** Configuración general (valores por defecto del reparto, email, seguridad). */
public interface ConfiguracionService {

	/** Todos los parámetros, o solo los de una sección (prefijo, p. ej. "smtp."). */
	List<ConfiguracionDto> listar(String prefijo);

	ConfiguracionDto actualizar(String clave, ConfiguracionRequest request);

	Optional<Integer> entero(String clave);

	Optional<String> texto(String clave);

}
