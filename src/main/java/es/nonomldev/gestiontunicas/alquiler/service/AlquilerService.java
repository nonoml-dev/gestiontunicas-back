package es.nonomldev.gestiontunicas.alquiler.service;

import java.util.List;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDto;
import es.nonomldev.gestiontunicas.alquiler.dto.CambioTunicaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.EntregaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.ResguardoDto;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;

/** Entrega y cambio de túnicas durante el reparto, y sus resguardos. */
public interface AlquilerService {

	/**
	 * Diálogo "Entregar túnica": comprueba la situación del hermano y la jornada de hoy, bloquea la
	 * túnica, registra la entrega con la limosna aplicable y emite el resguardo.
	 */
	AlquilerDto entregar(EntregaRequest request);

	/** Diálogo "Cambiar túnica": devolución rápida de la actual, entrega de la nueva y nuevo resguardo. */
	AlquilerDto cambiar(Long alquilerId, CambioTunicaRequest request);

	AlquilerDto obtener(Long id);

	List<AlquilerDto> historialHermano(Long hermanoId);

	List<AlquilerDto> historialTunica(Long tunicaId);

	/** Entregas de un reparto en un estado (p. ej. pendientes de devolución = ACTIVO). */
	List<AlquilerDto> deReparto(Long repartoId, EstadoAlquiler estado);

	List<ResguardoDto> resguardos(Long alquilerId);

	ResguardoDto registrarImpresion(Long resguardoId);

	ResguardoDto anularResguardo(Long resguardoId, MotivoRequest request);

}
