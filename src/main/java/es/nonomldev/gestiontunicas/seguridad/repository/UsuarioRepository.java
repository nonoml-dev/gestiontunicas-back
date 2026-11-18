package es.nonomldev.gestiontunicas.seguridad.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	/** Para el login: trae los roles y sus permisos en la misma consulta. */
	@EntityGraph(attributePaths = { "roles", "roles.permisos" })
	Optional<Usuario> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByUsernameAndIdNot(String username, Long id);

}
