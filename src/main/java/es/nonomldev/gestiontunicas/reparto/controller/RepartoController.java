package es.nonomldev.gestiontunicas.reparto.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.reparto.dto.AsignarPlantillaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDTO;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDTO;
import es.nonomldev.gestiontunicas.reparto.service.RepartoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reparto anual: BORRADOR → ABIERTO → CERRADO. Lo gestionan Administrador y Prioste, incluidas la limosna
 * y los importes de sanción A–F; Mayordomo y Secretario lo consultan. El Capiller no tiene acceso: su
 * parte son las devoluciones (matriz del análisis §3).
 */
@RestController
@RequestMapping("/api/v1/repartos")
@RequiredArgsConstructor
public class RepartoController {

	static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'PRIOSTE')";
	static final String PUEDE_CONSULTAR = "hasAnyRole('ADMIN', 'PRIOSTE', 'MAYORDOMO', 'SECRETARIO')";

	private final RepartoService repartoService;

	/** Del más reciente al más antiguo. */
	@GetMapping
	@PreAuthorize(PUEDE_CONSULTAR)
	public List<RepartoDTO> listar() {
		return repartoService.listar();
	}

	/** El reparto en curso; 204 si no hay ninguno abierto. */
	@GetMapping("/abierto")
	@PreAuthorize(PUEDE_CONSULTAR)
	public ResponseEntity<RepartoDTO> abierto() {
		return repartoService.obtenerAbierto()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/{id}")
	@PreAuthorize(PUEDE_CONSULTAR)
	public RepartoDTO obtener(@PathVariable Long id) {
		return repartoService.obtener(id);
	}

	/** Nace en BORRADOR con sus 3 jornadas. Sin Jueves Santo, se calcula. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO crear(@Valid @RequestBody RepartoRequest request) {
		return repartoService.crear(request);
	}

	/** Solo en BORRADOR. */
	@PutMapping("/{id}")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO actualizar(@PathVariable Long id, @Valid @RequestBody RepartoRequest request) {
		return repartoService.actualizar(id, request);
	}

	/** Plantilla del resguardo vigente. Hace falta para abrir el reparto. */
	@PutMapping("/{id}/plantilla")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO asignarPlantilla(@PathVariable Long id, @Valid @RequestBody AsignarPlantillaRequest request) {
		return repartoService.asignarPlantilla(id, request.plantillaId());
	}

	/** Pasa a ABIERTO y reserva la túnica de quien renueva (salvo en modo transición). */
	@PostMapping("/{id}/abrir")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO abrir(@PathVariable Long id) {
		return repartoService.abrir(id);
	}

	/** Libera las reservas no recogidas (al empezar el día 3). */
	@PostMapping("/{id}/liberar-reservas")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO liberarReservas(@PathVariable Long id) {
		return repartoService.liberarReservas(id);
	}

	/** Pasa a CERRADO y libera las reservas que queden. */
	@PostMapping("/{id}/cerrar")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO cerrar(@PathVariable Long id) {
		return repartoService.cerrar(id);
	}

	/** Cambia el periodo de devolución o el plazo de gracia. Motivo obligatorio; queda en el historial. */
	@PatchMapping("/{id}/periodo-devolucion")
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO cambiarPeriodoDevolucion(@PathVariable Long id,
			@Valid @RequestBody CambioPeriodoDevolucionRequest request) {
		return repartoService.cambiarPeriodoDevolucion(id, request);
	}

	/** Cambios del periodo de devolución, del más reciente al más antiguo. */
	@GetMapping("/{id}/periodo-devolucion/cambios")
	@PreAuthorize(PUEDE_CONSULTAR)
	public List<CambioPeriodoDevolucionDTO> cambiosPeriodoDevolucion(@PathVariable Long id) {
		return repartoService.historialPeriodoDevolucion(id);
	}

	/** Jornada extraordinaria, con limosna propia opcional. */
	@PostMapping("/{id}/jornadas-extraordinarias")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(PUEDE_GESTIONAR)
	public RepartoDTO anadirJornadaExtraordinaria(@PathVariable Long id, @Valid @RequestBody JornadaRequest request) {
		return repartoService.anadirJornadaExtraordinaria(id, request);
	}

	/** Reservas activas (túnicas apartadas para quien renueva). */
	@GetMapping("/{id}/reservas")
	@PreAuthorize(PUEDE_CONSULTAR)
	public List<ReservaDTO> reservas(@PathVariable Long id) {
		return repartoService.reservas(id);
	}

}
