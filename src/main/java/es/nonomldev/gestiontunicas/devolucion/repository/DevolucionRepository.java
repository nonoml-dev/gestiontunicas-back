package es.nonomldev.gestiontunicas.devolucion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.devolucion.entity.DevolucionEntity;

public interface DevolucionRepository extends JpaRepository<DevolucionEntity, Long> {

	@EntityGraph(attributePaths = { "revisiones", "revisiones.pieza" })
	Optional<DevolucionEntity> findByAlquilerId(Long alquilerId);

	boolean existsByAlquilerId(Long alquilerId);

}
