package es.nonomldev.gestiontunicas.sancion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.sancion.entity.Veto;

public interface VetoRepository extends JpaRepository<Veto, Long> {

	/** ¿Está vetado para el reparto de ese año? (el veto dura un solo año) */
	boolean existsByHermanoIdAndAnioVetadoAndActivoTrue(Long hermanoId, Integer anioVetado);

	/** Para mostrar el motivo exacto en la ficha del hermano. */
	Optional<Veto> findFirstByHermanoIdAndAnioVetadoAndActivoTrue(Long hermanoId, Integer anioVetado);

	/** Pestaña "Vetos" de la ficha del hermano (incluye los ya pasados o levantados). */
	List<Veto> findByHermanoIdOrderByFechaDesc(Long hermanoId);

	/** Vetos que aplican en un reparto (listado de la pantalla de Sanciones). */
	@EntityGraph(attributePaths = "hermano")
	List<Veto> findByAnioVetadoAndActivoTrueOrderByFechaDesc(Integer anioVetado);

	boolean existsByAlquilerId(Long alquilerId);

}
