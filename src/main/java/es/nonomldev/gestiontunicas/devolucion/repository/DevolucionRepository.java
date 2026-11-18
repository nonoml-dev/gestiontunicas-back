package es.nonomldev.gestiontunicas.devolucion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.devolucion.entity.Devolucion;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

	@EntityGraph(attributePaths = { "revisiones", "revisiones.pieza" })
	Optional<Devolucion> findByAlquilerId(Long alquilerId);

	boolean existsByAlquilerId(Long alquilerId);

}
