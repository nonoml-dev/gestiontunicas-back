package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucion;

public interface CambioPeriodoDevolucionRepository extends JpaRepository<CambioPeriodoDevolucion, Long> {

	List<CambioPeriodoDevolucion> findByRepartoIdOrderByFechaDesc(Long repartoId);

}
