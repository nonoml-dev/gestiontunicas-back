package es.nonomldev.gestiontunicas.hermano.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.comun.exception.ParametroNoValidoException;
import es.nonomldev.gestiontunicas.hermano.dto.ContactoHermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.CriterioEdadDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;
import es.nonomldev.gestiontunicas.hermano.service.HermanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Censo de hermanos. Lo consultan todos los usuarios; lo gestionan Administrador, Prioste, Mayordomo
 * y Secretario (el Capiller solo consulta).
 */
@RestController
@RequestMapping("/api/v1/hermanos")
@RequiredArgsConstructor
public class HermanoController {

	static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'PRIOSTE', 'MAYORDOMO', 'SECRETARIO')";
	private static final int TAMANO_MAXIMO = 100;

	/** Columnas por las que se puede ordenar el listado. */
	private static final Map<String, Sort> ORDENES = Map.of(
			"numeroHermano", Sort.by(new Sort.Order(Sort.Direction.ASC, "numeroOrden", Sort.NullHandling.NULLS_LAST)),
			"nombreCompleto", Sort.by("apellidos", "nombre"),
			"fechaNacimiento", Sort.by("fechaNacimiento"));

	private final HermanoService hermanoService;

	/**
	 * Buscador por nombre y apellidos (sin tildes, coincidencia parcial). {@code orden} = numeroHermano,
	 * nombreCompleto o fechaNacimiento; {@code direccion} = asc o desc. Los que no tienen número van al final.
	 * Salen todos los hermanos; con {@code enEdad=true}, solo los que están en edad a Jueves Santo.
	 */
	@GetMapping
	public Page<HermanoResumenDTO> buscar(
			@RequestParam(name = "q", required = false) String texto,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "25") int tamano,
			@RequestParam(defaultValue = "nombreCompleto") String orden,
			@RequestParam(defaultValue = "asc") String direccion,
			@RequestParam(defaultValue = "false") boolean enEdad) {
		// Cada columna ordena solo por lo suyo; el id solo desempata filas iguales para que la paginación sea estable.
		// Un orden desconocido es un error: si se cambiara en silencio por otro, no se notaría el fallo.
		Sort sort = ORDENES.get(orden);
		if (sort == null) {
			throw new ParametroNoValidoException("Orden no válido: " + orden + ". Se admite " + String.join(", ", ORDENES.keySet()));
		}
		if (!"asc".equalsIgnoreCase(direccion) && !"desc".equalsIgnoreCase(direccion)) {
			throw new ParametroNoValidoException("Dirección no válida: " + direccion + ". Se admite asc o desc");
		}
		sort = "desc".equalsIgnoreCase(direccion) ? sort.descending() : sort.ascending();
		PageRequest pageable = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, TAMANO_MAXIMO),
				sort.and(Sort.by("id")));
		return hermanoService.buscar(texto, enEdad, pageable);
	}

	/** Jueves Santo y rango de edad del filtro "En edad", para mostrarlo en la pantalla. */
	@GetMapping("/criterio-edad")
	public CriterioEdadDTO criterioEdad() {
		return hermanoService.criterioEdad();
	}

	@GetMapping("/{id}")
	public HermanoDTO obtener(@PathVariable Long id) {
		return hermanoService.obtener(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(PUEDE_GESTIONAR)
	public HermanoDTO crear(@Valid @RequestBody HermanoRequest request) {
		return hermanoService.crear(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize(PUEDE_GESTIONAR)
	public HermanoDTO actualizarContacto(@PathVariable Long id, @Valid @RequestBody ContactoHermanoRequest request) {
		return hermanoService.actualizarContacto(id, request);
	}

	/** Baja lógica: deja de salir en el listado, pero se conserva su historial. */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize(PUEDE_GESTIONAR)
	public void darDeBaja(@PathVariable Long id) {
		hermanoService.desactivar(id);
	}

}
