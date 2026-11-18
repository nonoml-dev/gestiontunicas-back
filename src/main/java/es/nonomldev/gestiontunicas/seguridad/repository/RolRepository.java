package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.Rol;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;

public interface RolRepository extends JpaRepository<Rol, Long> {

	@EntityGraph(attributePaths = "permisos")
	Optional<Rol> findByCodigo(RolCodigo codigo);

	List<Rol> findByCodigoIn(Collection<RolCodigo> codigos);

	/** Matriz de permisos de la pantalla de Administración. */
	@EntityGraph(attributePaths = "permisos")
	List<Rol> findAllByOrderByCodigo();

}
