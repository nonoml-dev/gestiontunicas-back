package es.nonomldev.gestiontunicas.devolucion.service;

import java.util.List;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDto;

/**
 * Túnicas no devueltas, en dos pasos (C1, C2):
 * <ol>
 * <li>Al terminar el periodo de devolución: sanción C y veto para el año próximo.</li>
 * <li>Al terminar el plazo de gracia: cargo total F (se suma a C) y la túnica pasa a ser del hermano,
 * con lo que su código queda libre.</li>
 * </ol>
 * En los dos casos primero se lista y luego se confirma.
 */
public interface NoDevueltasService {

	/** Lo que se procesará al cerrar el periodo de devolución (túnicas aún entregadas). */
	List<AlquilerDto> pendientesFinPeriodo(Long repartoId);

	/** Devuelve cuántas túnicas se han procesado. */
	int procesarFinPeriodo(Long repartoId);

	/** Lo que se procesará al cerrar el plazo de gracia (siguen sin devolver). */
	List<AlquilerDto> pendientesFinPlazoGracia(Long repartoId);

	int procesarFinPlazoGracia(Long repartoId);

}
