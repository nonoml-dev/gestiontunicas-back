package es.nonomldev.gestiontunicas.importacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.importacion.entity.EstadoFila;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFila;

public interface ImportacionFilaRepository extends JpaRepository<ImportacionFila, Long> {

	/** Pestañas de revisión: correctas, con errores, posibles duplicados, incompletas. */
	List<ImportacionFila> findByImportacionIdAndEstadoOrderByNumFila(Long importacionId, EstadoFila estado);

	List<ImportacionFila> findByImportacionIdOrderByNumFila(Long importacionId);

}
