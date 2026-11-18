package es.nonomldev.gestiontunicas.seguridad.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.seguridad.entity.RegistroAuditoria;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAcceso;

/** Registro de inicios de sesión. Solo lo consulta el Administrador. */
public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

	/** Filtros de la pantalla: todos opcionales (null = sin filtro). */
	@Query("""
			select r from RegistroAuditoria r
			where (:username is null or lower(r.username) like lower(concat('%', :username, '%')))
			  and (:resultado is null or r.resultado = :resultado)
			  and (:desde is null or r.fecha >= :desde)
			  and (:hasta is null or r.fecha < :hasta)
			order by r.fecha desc
			""")
	Page<RegistroAuditoria> buscar(String username, ResultadoAcceso resultado, Instant desde, Instant hasta,
			Pageable pageable);

}
