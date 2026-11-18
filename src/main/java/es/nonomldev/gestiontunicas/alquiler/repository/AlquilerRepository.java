package es.nonomldev.gestiontunicas.alquiler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;

public interface AlquilerRepository extends JpaRepository<Alquiler, Long> {

	/** Túnica activa del hermano en el reparto (para saber si ya recogió y puede hacer un cambio). */
	@EntityGraph(attributePaths = { "hermano", "tunica", "jornada" })
	Optional<Alquiler> findFirstByRepartoIdAndHermanoIdAndEstado(Long repartoId, Long hermanoId, EstadoAlquiler estado);

	/** Quién tiene ahora una túnica (alquiler activo). */
	Optional<Alquiler> findFirstByTunicaIdAndEstado(Long tunicaId, EstadoAlquiler estado);

	/**
	 * Último alquiler del hermano en el reparto de un año (sin contar los sustituidos por un cambio).
	 * Sirve para decidir si renueva (año anterior) o es nueva alta (hace 2 o más años).
	 */
	@Query("""
			select a from Alquiler a
			where a.hermano.id = :hermanoId
			  and a.reparto.anio = :anio
			  and a.estado <> es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler.CAMBIADO
			order by a.fechaEntrega desc
			limit 1
			""")
	Optional<Alquiler> findUltimoDelAnio(Long hermanoId, Integer anio);

	/** Último alquiler del hermano en cualquier año (para proponer su antigua túnica). */
	Optional<Alquiler> findFirstByHermanoIdOrderByFechaEntregaDesc(Long hermanoId);

	/** Historial del hermano. */
	@EntityGraph(attributePaths = { "reparto", "tunica" })
	List<Alquiler> findByHermanoIdOrderByFechaEntregaDesc(Long hermanoId);

	/** Historial de la túnica: hermanos que la han tenido. */
	@EntityGraph(attributePaths = { "reparto", "hermano" })
	List<Alquiler> findByTunicaIdOrderByFechaEntregaDesc(Long tunicaId);

	/** Pendientes de devolución, no devueltas al cerrar el plazo, en plazo de gracia... */
	@EntityGraph(attributePaths = { "hermano", "tunica" })
	List<Alquiler> findByRepartoIdAndEstado(Long repartoId, EstadoAlquiler estado);

	long countByRepartoIdAndEstado(Long repartoId, EstadoAlquiler estado);

}
