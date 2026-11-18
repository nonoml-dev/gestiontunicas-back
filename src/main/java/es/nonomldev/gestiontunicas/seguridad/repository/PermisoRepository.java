package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.PermisoEntity;

public interface PermisoRepository extends JpaRepository<PermisoEntity, Long> {

	Optional<PermisoEntity> findByCodigo(String codigo);

	List<PermisoEntity> findByCodigoIn(Collection<String> codigos);

	List<PermisoEntity> findAllByOrderByModuloAscCodigoAsc();

}
