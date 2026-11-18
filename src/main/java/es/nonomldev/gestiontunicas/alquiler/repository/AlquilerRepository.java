package es.nonomldev.gestiontunicas.alquiler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;

public interface AlquilerRepository extends JpaRepository<AlquilerEntity, Long> {

	/** Túnica activa del hermano en el reparto (para saber si ya recogió y puede hacer un cambio). */
	@EntityGraph(attributePaths = { "hermano", "tunica", "jornada" })
	Optional<AlquilerEntity> findFirstByRepartoIdAndHermanoIdAndEstado(Long repartoId, Long hermanoId, EstadoAlquilerEnum estado);

	/** Quién tiene ahora una túnica (alquiler activo). */
	Optional<AlquilerEntity> findFirstByTunicaIdAndEstado(Long tunicaId, EstadoAlquilerEnum estado);

	/**
	 * Último alquiler del hermano en el reparto de un año (sin contar los sustituidos por un cambio).
	 * Sirve para decidir si renueva (año anterior) o es nueva alta (hace 2 o más años).
	 */
	@Query("""
			select a from AlquilerEntity a
			where a.hermano.id = :hermanoId
			  and a.reparto.anio = :anio
			  and a.estado <> es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum.CAMBIADO
			order by a.fechaEntrega desc
			limit 1
			""")
	Optional<AlquilerEntity> findUltimoDelAnio(Long hermanoId, Integer anio);

	/** Último alquiler del hermano en cualquier año (para proponer su antigua túnica). */
	Optional<AlquilerEntity> findFirstByHermanoIdOrderByFechaEntregaDesc(Long hermanoId);

	/** Historial del hermano. */
	@EntityGraph(attributePaths = { "reparto", "tunica" })
	List<AlquilerEntity> findByHermanoIdOrderByFechaEntregaDesc(Long hermanoId);

	/** Historial de la túnica: hermanos que la han tenido. */
	@EntityGraph(attributePaths = { "reparto", "hermano" })
	List<AlquilerEntity> findByTunicaIdOrderByFechaEntregaDesc(Long tunicaId);

	/** Pendientes de devolución, no devueltas al cerrar el plazo, en plazo de gracia... */
	@EntityGraph(attributePaths = { "hermano", "tunica" })
	List<AlquilerEntity> findByRepartoIdAndEstado(Long repartoId, EstadoAlquilerEnum estado);

	long countByRepartoIdAndEstado(Long repartoId, EstadoAlquilerEnum estado);

}
