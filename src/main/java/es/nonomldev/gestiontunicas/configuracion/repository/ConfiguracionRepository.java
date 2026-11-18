package es.nonomldev.gestiontunicas.configuracion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.configuracion.entity.Configuracion;

public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

	Optional<Configuracion> findByClave(String clave);

	/** Una sección de la configuración, p. ej. "smtp." o "reparto.". */
	List<Configuracion> findByClaveStartingWithOrderByClave(String prefijo);

}
