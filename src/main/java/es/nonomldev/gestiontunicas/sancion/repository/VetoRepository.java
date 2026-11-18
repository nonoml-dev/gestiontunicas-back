package es.nonomldev.gestiontunicas.sancion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.sancion.entity.VetoEntity;

public interface VetoRepository extends JpaRepository<VetoEntity, Long> {

	/** ¿Está vetado para el reparto de ese año? (el veto dura un solo año) */
	boolean existsByHermanoIdAndAnioVetadoAndActivoTrue(Long hermanoId, Integer anioVetado);

	/** Para mostrar el motivo exacto en la ficha del hermano. */
	Optional<VetoEntity> findFirstByHermanoIdAndAnioVetadoAndActivoTrue(Long hermanoId, Integer anioVetado);

	/** Pestaña "Vetos" de la ficha del hermano (incluye los ya pasados o levantados). */
	List<VetoEntity> findByHermanoIdOrderByFechaDesc(Long hermanoId);

	/** Vetos que aplican en un reparto (listado de la pantalla de Sanciones). */
	@EntityGraph(attributePaths = "hermano")
	List<VetoEntity> findByAnioVetadoAndActivoTrueOrderByFechaDesc(Integer anioVetado);

	boolean existsByAlquilerId(Long alquilerId);

}
