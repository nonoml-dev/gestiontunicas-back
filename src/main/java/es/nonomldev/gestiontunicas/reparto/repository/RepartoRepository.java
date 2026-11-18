package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;

public interface RepartoRepository extends JpaRepository<RepartoEntity, Long> {

	@EntityGraph(attributePaths = "jornadas")
	Optional<RepartoEntity> findByAnio(Integer anio);

	boolean existsByAnio(Integer anio);

	/** El reparto en curso (solo puede haber uno ABIERTO). */
	@EntityGraph(attributePaths = "jornadas")
	Optional<RepartoEntity> findFirstByEstado(EstadoRepartoEnum estado);

	List<RepartoEntity> findAllByOrderByAnioDesc();

	@EntityGraph(attributePaths = "jornadas")
	Optional<RepartoEntity> findWithJornadasById(Long id);

}
