package es.nonomldev.gestiontunicas.configuracion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.configuracion.entity.ConfiguracionEntity;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionEntity, Long> {

	Optional<ConfiguracionEntity> findByClave(String clave);

	/** Una sección de la configuración, p. ej. "smtp." o "reparto.". */
	List<ConfiguracionEntity> findByClaveStartingWithOrderByClave(String prefijo);

}
