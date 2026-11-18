package es.nonomldev.gestiontunicas.alquiler.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.alquiler.entity.ResguardoEntity;

public interface ResguardoRepository extends JpaRepository<ResguardoEntity, Long> {

	/** El resguardo vigente (no anulado) de un alquiler. */
	Optional<ResguardoEntity> findFirstByAlquilerIdAndAnuladoFalse(Long alquilerId);

	/** Búsqueda por número de resguardo en la pantalla de devoluciones (p. ej. "2027-0042"). */
	Optional<ResguardoEntity> findByCodigo(String codigo);

	List<ResguardoEntity> findByAlquilerIdOrderByFechaEmision(Long alquilerId);

}
