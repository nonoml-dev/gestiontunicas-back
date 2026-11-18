package es.nonomldev.gestiontunicas.importacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFilaEntity;

public interface ImportacionFilaRepository extends JpaRepository<ImportacionFilaEntity, Long> {

	/** Pestañas de revisión: correctas, con errores, posibles duplicados, incompletas. */
	List<ImportacionFilaEntity> findByImportacionIdAndEstadoOrderByNumFila(Long importacionId, EstadoFilaEnum estado);

	List<ImportacionFilaEntity> findByImportacionIdOrderByNumFila(Long importacionId);

}
