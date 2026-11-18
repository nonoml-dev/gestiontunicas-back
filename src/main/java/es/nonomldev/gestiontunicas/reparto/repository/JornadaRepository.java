package es.nonomldev.gestiontunicas.reparto.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;

public interface JornadaRepository extends JpaRepository<JornadaEntity, Long> {

	List<JornadaEntity> findByRepartoIdOrderByNumero(Long repartoId);

	/** Jornada del día (para registrar en qué jornada se hizo cada entrega). */
	Optional<JornadaEntity> findByRepartoIdAndFecha(Long repartoId, LocalDate fecha);

	/** Siguiente número al habilitar una jornada extraordinaria. */
	@Query("select coalesce(max(j.numero), 0) from JornadaEntity j where j.reparto.id = :repartoId")
	int maxNumero(Long repartoId);

}
