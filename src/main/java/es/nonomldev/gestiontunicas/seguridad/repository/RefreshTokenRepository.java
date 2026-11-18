package es.nonomldev.gestiontunicas.seguridad.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.seguridad.entity.RefreshTokenEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	/** Cierra todas las sesiones de un usuario (logout global, cambio de contraseña, desactivación). */
	@Modifying
	@Query("update RefreshTokenEntity t set t.revocado = true where t.usuario.id = :usuarioId and t.revocado = false")
	int revocarTodosDeUsuario(Long usuarioId);

	/** Limpieza periódica de tokens caducados. */
	@Modifying
	@Query("delete from RefreshTokenEntity t where t.expiraEn < :fecha")
	int borrarCaducados(Instant fecha);

}
