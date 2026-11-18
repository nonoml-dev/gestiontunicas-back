package es.nonomldev.gestiontunicas.alquiler.service;

import java.util.List;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDTO;
import es.nonomldev.gestiontunicas.alquiler.dto.CambioTunicaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.EntregaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.ResguardoDTO;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;

/** Entrega y cambio de túnicas durante el reparto, y sus resguardos. */
public interface AlquilerService {

	/**
	 * Diálogo "Entregar túnica": comprueba la situación del hermano y la jornada de hoy, bloquea la
	 * túnica, registra la entrega con la limosna aplicable y emite el resguardo.
	 */
	AlquilerDTO entregar(EntregaRequest request);

	/** Diálogo "Cambiar túnica": devolución rápida de la actual, entrega de la nueva y nuevo resguardo. */
	AlquilerDTO cambiar(Long alquilerId, CambioTunicaRequest request);

	AlquilerDTO obtener(Long id);

	List<AlquilerDTO> historialHermano(Long hermanoId);

	List<AlquilerDTO> historialTunica(Long tunicaId);

	/** Entregas de un reparto en un estado (p. ej. pendientes de devolución = ACTIVO). */
	List<AlquilerDTO> deReparto(Long repartoId, EstadoAlquilerEnum estado);

	List<ResguardoDTO> resguardos(Long alquilerId);

	ResguardoDTO registrarImpresion(Long resguardoId);

	ResguardoDTO anularResguardo(Long resguardoId, MotivoRequest request);

}
