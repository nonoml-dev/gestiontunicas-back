package es.nonomldev.gestiontunicas.reparto.service;

import java.util.List;
import java.util.Optional;

import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDTO;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDTO;

/** Ciclo de vida del reparto anual: BORRADOR → ABIERTO → CERRADO. */
public interface RepartoService {

	List<RepartoDTO> listar();

	RepartoDTO obtener(Long id);

	/** El reparto en curso, si lo hay. */
	Optional<RepartoDTO> obtenerAbierto();

	/** Crea el reparto en BORRADOR con sus 3 jornadas ordinarias. Si no viene el Jueves Santo, se calcula. */
	RepartoDTO crear(RepartoRequest request);

	/** Solo mientras está en BORRADOR. */
	RepartoDTO actualizar(Long id, RepartoRequest request);

	/** Plantilla Word vigente: las entregas siguientes usarán esta. */
	RepartoDTO asignarPlantilla(Long id, Long plantillaId);

	/**
	 * Abre el reparto y reserva la túnica de cada hermano que renueva (la devolvió en plazo el año
	 * anterior y cumple los requisitos). En modo transición 2027 no se reserva nada.
	 */
	RepartoDTO abrir(Long id);

	/** Al empezar el día 3: las reservas no recogidas se liberan y sus túnicas quedan disponibles. */
	RepartoDTO liberarReservas(Long id);

	/**
	 * Liberación automática: si el reparto abierto ya ha llegado al día 3 y sus reservas siguen sin liberar,
	 * las libera. Devuelve el reparto si lo ha hecho; vacío si no tocaba o ya estaban liberadas.
	 */
	Optional<RepartoDTO> liberarReservasSiEmpiezaElDia3();

	RepartoDTO cerrar(Long id);

	/** Cambio del periodo de devolución o del plazo de gracia. Queda auditado con su motivo. */
	RepartoDTO cambiarPeriodoDevolucion(Long id, CambioPeriodoDevolucionRequest request);

	/** Jornada extraordinaria, con limosna propia opcional. */
	RepartoDTO anadirJornadaExtraordinaria(Long id, JornadaRequest request);

	List<CambioPeriodoDevolucionDTO> historialPeriodoDevolucion(Long id);

	List<ReservaDTO> reservas(Long id);

}
