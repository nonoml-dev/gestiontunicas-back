package es.nonomldev.gestiontunicas.tunica.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.ParametroNoValidoException;
import es.nonomldev.gestiontunicas.tunica.dto.AvisoCodigoDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDTO;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.service.TunicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Inventario de túnicas. Lo consultan todos los usuarios; lo gestionan Administrador y Prioste
 * (matriz del análisis §3).
 */
@RestController
@RequestMapping("/api/v1/tunicas")
@RequiredArgsConstructor
public class TunicaController {

	static final String PUEDE_GESTIONAR = "hasAnyRole('ADMIN', 'PRIOSTE')";
	private static final int TAMANO_MAXIMO = 100;

	/** Mismos límites que la medida de la túnica ({@link TunicaRequest}). */
	private static final int ALTURA_MINIMA = 60;
	private static final int ALTURA_MAXIMA = 220;
	private static final int TOLERANCIA_MAXIMA = 50;

	/**
	 * Columnas por las que se puede ordenar. El código ordena como número (2 antes que 10) y las de
	 * monaguillo (M-XXX) van detrás de las numéricas.
	 */
	private static final Map<String, Sort> ORDENES = Map.of(
			"codigo", JpaSort.unsafe("length(t.codigo)").and(Sort.by("codigo")),
			"medidaTotal", Sort.by("medidaTotalCm"),
			"estado", Sort.by("estado"));

	private final TunicaService tunicaService;

	/**
	 * Inventario paginado. {@code estado} admite varios valores; sin él salen todas menos las que son
	 * propiedad de un hermano. {@code medidaMin} y {@code medidaMax} filtran por medida total.
	 * {@code orden} = codigo, medidaTotal o estado; {@code direccion} = asc o desc.
	 */
	@GetMapping
	public Page<TunicaResumenDTO> buscar(
			@RequestParam(name = "estado", required = false) Set<EstadoTunicaEnum> estados,
			@RequestParam(required = false) Integer medidaMin,
			@RequestParam(required = false) Integer medidaMax,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "25") int tamano,
			@RequestParam(defaultValue = "codigo") String orden,
			@RequestParam(defaultValue = "asc") String direccion) {
		if (medidaMin != null && medidaMax != null && medidaMin > medidaMax) {
			throw new ParametroNoValidoException("La medida mínima no puede ser mayor que la máxima");
		}
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
		return tunicaService.buscar(estados, medidaMin, medidaMax, pageable);
	}

	/**
	 * Túnicas disponibles que encajan con la altura del hermano, de la más cercana a la más lejana.
	 * Sin {@code toleranciaCm} se usa la del reparto abierto; con ella, el prioste amplía la búsqueda.
	 */
	@GetMapping("/propuesta")
	public List<TunicaResumenDTO> proponer(
			@RequestParam int alturaCm,
			@RequestParam(required = false) Integer toleranciaCm) {
		if (alturaCm < ALTURA_MINIMA || alturaCm > ALTURA_MAXIMA) {
			throw new ParametroNoValidoException("La altura debe estar entre %d y %d cm".formatted(ALTURA_MINIMA, ALTURA_MAXIMA));
		}
		if (toleranciaCm != null && (toleranciaCm < 0 || toleranciaCm > TOLERANCIA_MAXIMA)) {
			throw new ParametroNoValidoException("La tolerancia debe estar entre 0 y %d cm".formatted(TOLERANCIA_MAXIMA));
		}
		return tunicaService.proponer(alturaCm, toleranciaCm);
	}

	/** Aviso al dar de alta: el código perteneció a una túnica que ahora es de un hermano. */
	@GetMapping("/aviso-codigo")
	@PreAuthorize(PUEDE_GESTIONAR)
	public AvisoCodigoDTO avisoCodigo(@RequestParam String codigo) {
		return new AvisoCodigoDTO(codigo, tunicaService.codigoFueDeHermano(codigo));
	}

	@GetMapping("/{id}")
	public TunicaDTO obtener(@PathVariable Long id) {
		return tunicaService.obtener(id);
	}

	/** Alta con sus 5 piezas en buen estado. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize(PUEDE_GESTIONAR)
	public TunicaDTO crear(@Valid @RequestBody TunicaRequest request) {
		return tunicaService.crear(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize(PUEDE_GESTIONAR)
	public TunicaDTO actualizar(@PathVariable Long id, @Valid @RequestBody TunicaRequest request) {
		return tunicaService.actualizar(id, request);
	}

	/** Baja con motivo obligatorio. La túnica se conserva con su historial. */
	@PostMapping("/{id}/baja")
	@PreAuthorize(PUEDE_GESTIONAR)
	public TunicaDTO darDeBaja(@PathVariable Long id, @Valid @RequestBody MotivoRequest request) {
		return tunicaService.darDeBaja(id, request);
	}

}
