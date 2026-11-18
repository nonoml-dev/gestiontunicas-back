package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;

public interface RepartoRepository extends JpaRepository<Reparto, Long> {

	@EntityGraph(attributePaths = "jornadas")
	Optional<Reparto> findByAnio(Integer anio);

	boolean existsByAnio(Integer anio);

	/** El reparto en curso (solo puede haber uno ABIERTO). */
	@EntityGraph(attributePaths = "jornadas")
	Optional<Reparto> findFirstByEstado(EstadoReparto estado);

	List<Reparto> findAllByOrderByAnioDesc();

	@EntityGraph(attributePaths = "jornadas")
	Optional<Reparto> findWithJornadasById(Long id);

}
