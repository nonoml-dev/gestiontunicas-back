package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucionEntity;

public interface CambioPeriodoDevolucionRepository extends JpaRepository<CambioPeriodoDevolucionEntity, Long> {

	List<CambioPeriodoDevolucionEntity> findByRepartoIdOrderByFechaDesc(Long repartoId);

}
