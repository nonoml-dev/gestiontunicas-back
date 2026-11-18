package es.nonomldev.gestiontunicas.seguridad.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.seguridad.entity.RegistroAuditoriaEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;

/** Registro de inicios de sesión. Solo lo consulta el Administrador. */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoriaEntity, Long> {

	/**
	 * Filtros de la pantalla. {@code username} y {@code resultado} son opcionales (null = sin filtro).
	 * {@code desde} y {@code hasta} son obligatorios: PostgreSQL no sabe de qué tipo es una fecha en
	 * {@code :param is null} y la consulta falla, así que el servicio pone límites amplios si no se filtra.
	 */
	@Query("""
			select r from RegistroAuditoriaEntity r
			where (:username is null or lower(r.username) like lower(concat('%', :username, '%')))
			  and (:resultado is null or r.resultado = :resultado)
			  and r.fecha >= :desde
			  and r.fecha < :hasta
			order by r.fecha desc
			""")
	Page<RegistroAuditoriaEntity> buscar(String username, ResultadoAccesoEnum resultado, Instant desde, Instant hasta,
			Pageable pageable);

}
