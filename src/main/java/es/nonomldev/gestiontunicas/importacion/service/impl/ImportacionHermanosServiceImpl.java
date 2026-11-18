package es.nonomldev.gestiontunicas.importacion.service.impl;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDto;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDto;
import es.nonomldev.gestiontunicas.importacion.entity.DecisionFila;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFila;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacion;
import es.nonomldev.gestiontunicas.importacion.entity.Importacion;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFila;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacion;
import es.nonomldev.gestiontunicas.importacion.mapper.ImportacionMapper;
import es.nonomldev.gestiontunicas.importacion.repository.ImportacionFilaRepository;
import es.nonomldev.gestiontunicas.importacion.repository.ImportacionRepository;
import es.nonomldev.gestiontunicas.importacion.service.ImportacionHermanosService;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelHermanos;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelHermanos.FilaLeida;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportacionHermanosServiceImpl implements ImportacionHermanosService {

	private static final String APELLIDOS = "apellidos";
	private static final String NOMBRE = "nombre";
	private static final String NUMERO = "numeroHermano";
	private static final String FECHA = "fechaNacimiento";

	private final LectorExcelHermanos lector;
	private final ImportacionRepository importacionRepository;
	private final ImportacionFilaRepository filaRepository;
	private final HermanoRepository hermanoRepository;
	private final RepartoRepository repartoRepository;
	private final ImportacionMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	public ImportacionDto previsualizar(String nombreFichero, InputStream excel, Long repartoId) {
		List<FilaLeida> leidas = lector.leer(excel);
		if (leidas.isEmpty()) {
			throw new ReglaNegocioException("El fichero no tiene ninguna fila con datos");
		}

		Importacion importacion = new Importacion();
		importacion.setTipo(TipoImportacion.HERMANOS);
		importacion.setNombreFichero(nombreFichero);
		importacion.setEstado(EstadoImportacion.PREVISUALIZADA);
		importacion.setUsuario(usuarioActualService.obtener());
		importacion.setCreadoEn(Instant.now(clock));
		if (repartoId != null) {
			importacion.setReparto(repartoRepository.findById(repartoId)
					.orElseThrow(() -> new RecursoNoEncontradoException("el reparto", repartoId)));
		}

		leidas.forEach(leida -> importacion.addFila(clasificar(leida)));
		importacion.recalcularContadores();
		return mapper.toDto(importacionRepository.save(importacion));
	}

	@Override
	@Transactional(readOnly = true)
	public ImportacionDto obtener(Long id) {
		return mapper.toDto(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ImportacionFilaDto> filas(Long importacionId, EstadoFila estado) {
		List<ImportacionFila> filas = estado == null
				? filaRepository.findByImportacionIdOrderByNumFila(importacionId)
				: filaRepository.findByImportacionIdAndEstadoOrderByNumFila(importacionId, estado);
		return mapper.toFilaDtos(filas);
	}

	@Override
	public ImportacionFilaDto decidir(Long filaId, DecisionFilaRequest request) {
		ImportacionFila fila = filaRepository.findById(filaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la fila de importación", filaId));
		exigirPrevisualizada(fila.getImportacion());
		if (fila.getEstado() == EstadoFila.ERROR) {
			throw new ReglaNegocioException("La fila %d tiene errores y no se puede importar".formatted(fila.getNumFila()));
		}
		if (request.decision() == DecisionFila.ACTUALIZAR && fila.getHermanoCoincidente() == null) {
			throw new ReglaNegocioException("La fila %d no coincide con ningún hermano existente".formatted(fila.getNumFila()));
		}
		fila.setDecision(request.decision());
		return mapper.toDto(fila);
	}

	@Override
	public ImportacionDto confirmar(Long id) {
		Importacion importacion = buscar(id);
		exigirPrevisualizada(importacion);

		long sinDecidir = importacion.getFilas().stream()
				.filter(f -> f.getEstado() != EstadoFila.ERROR && f.getDecision() == null)
				.count();
		if (sinDecidir > 0) {
			throw new ReglaNegocioException("Quedan %d posibles duplicados sin decidir".formatted(sinDecidir));
		}

		for (ImportacionFila fila : importacion.getFilas()) {
			if (fila.getEstado() == EstadoFila.ERROR || fila.getDecision() == DecisionFila.IGNORAR) {
				continue;
			}
			Hermano hermano = fila.getDecision() == DecisionFila.ACTUALIZAR
					? actualizarHermano(fila)
					: crearHermano(fila, importacion);
			fila.setEntidadResultanteId(hermano.getId());
		}
		importacion.setEstado(EstadoImportacion.CONFIRMADA);
		importacion.setConfirmadoEn(Instant.now(clock));
		return mapper.toDto(importacion);
	}

	@Override
	public ImportacionDto cancelar(Long id) {
		Importacion importacion = buscar(id);
		exigirPrevisualizada(importacion);
		importacion.setEstado(EstadoImportacion.CANCELADA);
		return mapper.toDto(importacion);
	}

	/**
	 * Clasifica la fila:
	 * <ul>
	 * <li>Con errores → ERROR.</li>
	 * <li>Sin fecha de nacimiento → INCOMPLETA (se crea; no podrá alquilar hasta completarla).</li>
	 * <li>Mismo nombre y fecha que un hermano → OK, se actualiza (número de hermano).</li>
	 * <li>Mismo nombre con otra fecha, o varias coincidencias → POSIBLE_DUPLICADO (decide el usuario).</li>
	 * <li>Sin coincidencias → OK, se crea.</li>
	 * </ul>
	 */
	private ImportacionFila clasificar(FilaLeida leida) {
		ImportacionFila fila = new ImportacionFila();
		fila.setNumFila(leida.numFila());
		fila.setDatos(datos(leida));
		fila.setErrores(leida.errores());

		if (!leida.errores().isEmpty()) {
			fila.setEstado(EstadoFila.ERROR);
			return fila;
		}

		String normalizado = Hermano.normalizar(leida.apellidos() + " " + leida.nombre());
		if (leida.fechaNacimiento() == null) {
			List<Hermano> mismoNombre = hermanoRepository.findByNombreNormalizado(normalizado);
			fila.setEstado(mismoNombre.isEmpty() ? EstadoFila.INCOMPLETA : EstadoFila.POSIBLE_DUPLICADO);
			fila.setDecision(mismoNombre.isEmpty() ? DecisionFila.CREAR : null);
			fila.setHermanoCoincidente(mismoNombre.isEmpty() ? null : mismoNombre.getFirst());
			return fila;
		}

		List<Hermano> exactos = hermanoRepository.findByNombreNormalizadoAndFechaNacimiento(normalizado,
				leida.fechaNacimiento());
		if (exactos.size() == 1) {
			fila.setEstado(EstadoFila.OK);
			fila.setDecision(DecisionFila.ACTUALIZAR);
			fila.setHermanoCoincidente(exactos.getFirst());
			return fila;
		}
		if (exactos.size() > 1) {
			fila.setEstado(EstadoFila.POSIBLE_DUPLICADO);
			fila.setHermanoCoincidente(exactos.getFirst());
			return fila;
		}

		List<Hermano> mismoNombre = hermanoRepository.findByNombreNormalizado(normalizado);
		if (!mismoNombre.isEmpty()) {
			fila.setEstado(EstadoFila.POSIBLE_DUPLICADO);
			fila.setHermanoCoincidente(mismoNombre.getFirst());
			return fila;
		}

		fila.setEstado(EstadoFila.OK);
		fila.setDecision(DecisionFila.CREAR);
		return fila;
	}

	private Hermano crearHermano(ImportacionFila fila, Importacion importacion) {
		Map<String, Object> datos = fila.getDatos();
		Hermano hermano = new Hermano();
		hermano.setApellidos((String) datos.get(APELLIDOS));
		hermano.setNombre((String) datos.get(NOMBRE));
		hermano.setNumeroHermano((String) datos.get(NUMERO));
		hermano.setFechaNacimiento(fecha(datos));
		hermano.setActivo(true);
		hermano.setImportacion(importacion);
		return hermanoRepository.save(hermano);
	}

	/** El número de hermano cambia cada año: se actualiza. La fecha solo se completa si faltaba. */
	private Hermano actualizarHermano(ImportacionFila fila) {
		Hermano hermano = fila.getHermanoCoincidente();
		hermano.setNumeroHermano((String) fila.getDatos().get(NUMERO));
		if (hermano.getFechaNacimiento() == null) {
			hermano.setFechaNacimiento(fecha(fila.getDatos()));
		}
		hermano.setActivo(true);
		return hermano;
	}

	private static Map<String, Object> datos(FilaLeida leida) {
		Map<String, Object> datos = new HashMap<>();
		datos.put(APELLIDOS, leida.apellidos());
		datos.put(NOMBRE, leida.nombre());
		datos.put(NUMERO, leida.numeroHermano());
		datos.put(FECHA, leida.fechaNacimiento() == null ? null : leida.fechaNacimiento().toString());
		return datos;
	}

	private static LocalDate fecha(Map<String, Object> datos) {
		Object valor = datos.get(FECHA);
		return valor == null ? null : LocalDate.parse(valor.toString());
	}

	private Importacion buscar(Long id) {
		return importacionRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("la importación", id));
	}

	private static void exigirPrevisualizada(Importacion importacion) {
		if (importacion.getEstado() != EstadoImportacion.PREVISUALIZADA) {
			throw new ReglaNegocioException("La importación ya está " + importacion.getEstado().name().toLowerCase());
		}
	}

}
