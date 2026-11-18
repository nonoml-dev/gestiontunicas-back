package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

	Optional<Permiso> findByCodigo(String codigo);

	List<Permiso> findByCodigoIn(Collection<String> codigos);

	List<Permiso> findAllByOrderByModuloAscCodigoAsc();

}
