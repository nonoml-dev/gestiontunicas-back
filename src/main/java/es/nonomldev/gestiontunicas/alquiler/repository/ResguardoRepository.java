package es.nonomldev.gestiontunicas.alquiler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.alquiler.entity.Resguardo;

public interface ResguardoRepository extends JpaRepository<Resguardo, Long> {

	/** El resguardo vigente (no anulado) de un alquiler. */
	Optional<Resguardo> findFirstByAlquilerIdAndAnuladoFalse(Long alquilerId);

	/** Búsqueda por número de resguardo en la pantalla de devoluciones (p. ej. "2027-0042"). */
	Optional<Resguardo> findByCodigo(String codigo);

	List<Resguardo> findByAlquilerIdOrderByFechaEmision(Long alquilerId);

}
