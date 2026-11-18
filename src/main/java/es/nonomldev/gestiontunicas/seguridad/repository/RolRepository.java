package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

	@EntityGraph(attributePaths = "permisos")
	Optional<RolEntity> findByCodigo(RolCodigoEnum codigo);

	List<RolEntity> findByCodigoIn(Collection<RolCodigoEnum> codigos);

	/** Matriz de permisos de la pantalla de Administración. */
	@EntityGraph(attributePaths = "permisos")
	List<RolEntity> findAllByOrderByCodigo();

}
