package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

	/** Para el login: trae los roles y sus permisos en la misma consulta. */
	@EntityGraph(attributePaths = { "roles", "roles.permisos" })
	Optional<UsuarioEntity> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByUsernameAndIdNot(String username, Long id);

	/** Si hay algún usuario activo con ese rol (p. ej. para saber si ya existe un administrador). */
	boolean existsByActivoTrueAndRoles_Codigo(RolCodigoEnum codigo);

}
