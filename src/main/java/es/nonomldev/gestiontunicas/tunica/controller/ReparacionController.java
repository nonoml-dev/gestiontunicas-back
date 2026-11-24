package es.nonomldev.gestiontunicas.tunica.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDTO;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;
import es.nonomldev.gestiontunicas.tunica.service.ReparacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reparaciones y reposiciones de las piezas de una túnica. Las consultan todos los usuarios; las registran
 * Administrador y Prioste, igual que el resto del inventario (matriz del análisis §3).
 */
@RestController
@RequestMapping("/api/v1/tunicas/{tunicaId}/reparaciones")
@RequiredArgsConstructor
public class ReparacionController {

	private final ReparacionService reparacionService;

	/** De la más reciente a la más antigua. */
	@GetMapping
	public List<ReparacionDTO> listar(@PathVariable Long tunicaId) {
		return reparacionService.deTunica(tunicaId);
	}

	/** La pieza pasa a "en reparación" y la túnica, al estado que corresponde a sus piezas. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(TunicaController.PUEDE_GESTIONAR)
	public ReparacionDTO registrar(@PathVariable Long tunicaId, @Valid @RequestBody ReparacionRequest request) {
		return reparacionService.registrar(tunicaId, request);
	}

	/** Cambiar el estado (en curso, terminada), las fechas, la descripción o el coste. */
	@PutMapping("/{id}")
	@PreAuthorize(TunicaController.PUEDE_GESTIONAR)
	public ReparacionDTO actualizar(@PathVariable Long tunicaId, @PathVariable Long id,
			@Valid @RequestBody ReparacionRequest request) {
		return reparacionService.actualizar(tunicaId, id, request);
	}

}
