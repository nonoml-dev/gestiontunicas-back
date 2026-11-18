package es.nonomldev.gestiontunicas.reparto.service;

import java.util.List;
import java.util.Optional;

import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDto;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDto;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDto;

/** Ciclo de vida del reparto anual: BORRADOR → ABIERTO → CERRADO. */
public interface RepartoService {

	List<RepartoDto> listar();

	RepartoDto obtener(Long id);

	/** El reparto en curso, si lo hay. */
	Optional<RepartoDto> obtenerAbierto();

	/** Crea el reparto en BORRADOR con sus 3 jornadas ordinarias. Si no viene el Jueves Santo, se calcula. */
	RepartoDto crear(RepartoRequest request);

	/** Solo mientras está en BORRADOR. */
	RepartoDto actualizar(Long id, RepartoRequest request);

	/** Plantilla Word vigente: las entregas siguientes usarán esta. */
	RepartoDto asignarPlantilla(Long id, Long plantillaId);

	/**
	 * Abre el reparto y reserva la túnica de cada hermano que renueva (la devolvió en plazo el año
	 * anterior y cumple los requisitos). En modo transición 2027 no se reserva nada.
	 */
	RepartoDto abrir(Long id);

	/** Al empezar el día 3: las reservas no recogidas se liberan y sus túnicas quedan disponibles. */
	RepartoDto liberarReservas(Long id);

	RepartoDto cerrar(Long id);

	/** Cambio del periodo de devolución o del plazo de gracia. Queda auditado con su motivo. */
	RepartoDto cambiarPeriodoDevolucion(Long id, CambioPeriodoDevolucionRequest request);

	/** Jornada extraordinaria, con limosna propia opcional. */
	RepartoDto anadirJornadaExtraordinaria(Long id, JornadaRequest request);

	List<CambioPeriodoDevolucionDto> historialPeriodoDevolucion(Long id);

	List<ReservaDto> reservas(Long id);

}
