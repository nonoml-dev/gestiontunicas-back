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
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDTO;
import es.nonomldev.gestiontunicas.importacion.entity.DecisionFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionEntity;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFilaEntity;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacionEnum;
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

	private static final String NOMBRE_ORIGINAL = "nombreOriginal";
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
	public ImportacionDTO previsualizar(String nombreFichero, InputStream excel, Long repartoId) {
		List<FilaLeida> leidas = lector.leer(excel);
		if (leidas.isEmpty()) {
			throw new ReglaNegocioException("El fichero no tiene ninguna fila con datos");
		}

		ImportacionEntity importacion = new ImportacionEntity();
		importacion.setTipo(TipoImportacionEnum.HERMANOS);
		importacion.setNombreFichero(nombreFichero);
		importacion.setEstado(EstadoImportacionEnum.PREVISUALIZADA);
		importacion.setUsuario(usuarioActualService.obtener());
		importacion.setCreadoEn(Instant.now(clock));
		if (repartoId != null) {
			importacion.setReparto(repartoRepository.findById(repartoId)
					.orElseThrow(() -> new RecursoNoEncontradoException("el reparto", repartoId)));
		}

		leidas.forEach(leida -> importacion.addFila(clasificar(leida)));
		importacion.recalcularContadores();
		return mapper.toDTO(importacionRepository.save(importacion));
	}

	@Override
	@Transactional(readOnly = true)
	public ImportacionDTO obtener(Long id) {
		return mapper.toDTO(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ImportacionFilaDTO> filas(Long importacionId, EstadoFilaEnum estado) {
		buscar(importacionId);
		List<ImportacionFilaEntity> filas = estado == null
				? filaRepository.findByImportacionIdOrderByNumFila(importacionId)
				: filaRepository.findByImportacionIdAndEstadoOrderByNumFila(importacionId, estado);
		return mapper.toFilaDTOs(filas);
	}

	@Override
	public ImportacionFilaDTO decidir(Long filaId, DecisionFilaRequest request) {
		ImportacionFilaEntity fila = filaRepository.findById(filaId)
				.filter(f -> f.getImportacion().getTipo() == TipoImportacionEnum.HERMANOS)
				.orElseThrow(() -> new RecursoNoEncontradoException("la fila de importación", filaId));
		exigirPrevisualizada(fila.getImportacion());
		if (fila.getEstado() == EstadoFilaEnum.ERROR) {
			throw new ReglaNegocioException("La fila %d tiene errores y no se puede importar".formatted(fila.getNumFila()));
		}
		if (request.decision() == DecisionFilaEnum.ACTUALIZAR && fila.getHermanoCoincidente() == null) {
			throw new ReglaNegocioException("La fila %d no coincide con ningún hermano existente".formatted(fila.getNumFila()));
		}
		fila.setDecision(request.decision());
		return mapper.toDTO(fila);
	}

	@Override
	public ImportacionDTO confirmar(Long id) {
		ImportacionEntity importacion = buscar(id);
		exigirPrevisualizada(importacion);

		long sinDecidir = importacion.getFilas().stream()
				.filter(f -> f.getEstado() != EstadoFilaEnum.ERROR && f.getDecision() == null)
				.count();
		if (sinDecidir > 0) {
			throw new ReglaNegocioException("Quedan %d posibles duplicados sin decidir".formatted(sinDecidir));
		}

		for (ImportacionFilaEntity fila : importacion.getFilas()) {
			if (fila.getEstado() == EstadoFilaEnum.ERROR || fila.getDecision() == DecisionFilaEnum.IGNORAR) {
				continue;
			}
			HermanoEntity hermano = fila.getDecision() == DecisionFilaEnum.ACTUALIZAR
					? actualizarHermano(fila)
					: crearHermano(fila, importacion);
			fila.setEntidadResultanteId(hermano.getId());
		}
		importacion.setEstado(EstadoImportacionEnum.CONFIRMADA);
		importacion.setConfirmadoEn(Instant.now(clock));
		return mapper.toDTO(importacion);
	}

	@Override
	public ImportacionDTO cancelar(Long id) {
		ImportacionEntity importacion = buscar(id);
		exigirPrevisualizada(importacion);
		importacion.setEstado(EstadoImportacionEnum.CANCELADA);
		return mapper.toDTO(importacion);
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] plantilla() {
		return lector.plantilla();
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
	private ImportacionFilaEntity clasificar(FilaLeida leida) {
		ImportacionFilaEntity fila = new ImportacionFilaEntity();
		fila.setNumFila(leida.numFila());
		fila.setDatos(datos(leida));
		fila.setErrores(leida.errores());

		if (!leida.errores().isEmpty()) {
			fila.setEstado(EstadoFilaEnum.ERROR);
			return fila;
		}

		String normalizado = HermanoEntity.normalizar(leida.apellidos() + " " + leida.nombre());
		if (leida.fechaNacimiento() == null) {
			List<HermanoEntity> mismoNombre = hermanoRepository.findByNombreNormalizado(normalizado);
			fila.setEstado(mismoNombre.isEmpty() ? EstadoFilaEnum.INCOMPLETA : EstadoFilaEnum.POSIBLE_DUPLICADO);
			fila.setDecision(mismoNombre.isEmpty() ? DecisionFilaEnum.CREAR : null);
			fila.setHermanoCoincidente(mismoNombre.isEmpty() ? null : mismoNombre.getFirst());
			return fila;
		}

		List<HermanoEntity> exactos = hermanoRepository.findByNombreNormalizadoAndFechaNacimiento(normalizado,
				leida.fechaNacimiento());
		if (exactos.size() == 1) {
			fila.setEstado(EstadoFilaEnum.OK);
			fila.setDecision(DecisionFilaEnum.ACTUALIZAR);
			fila.setHermanoCoincidente(exactos.getFirst());
			return fila;
		}
		if (exactos.size() > 1) {
			fila.setEstado(EstadoFilaEnum.POSIBLE_DUPLICADO);
			fila.setHermanoCoincidente(exactos.getFirst());
			return fila;
		}

		List<HermanoEntity> mismoNombre = hermanoRepository.findByNombreNormalizado(normalizado);
		if (!mismoNombre.isEmpty()) {
			fila.setEstado(EstadoFilaEnum.POSIBLE_DUPLICADO);
			fila.setHermanoCoincidente(mismoNombre.getFirst());
			return fila;
		}

		fila.setEstado(EstadoFilaEnum.OK);
		fila.setDecision(DecisionFilaEnum.CREAR);
		return fila;
	}

	private HermanoEntity crearHermano(ImportacionFilaEntity fila, ImportacionEntity importacion) {
		Map<String, Object> datos = fila.getDatos();
		HermanoEntity hermano = new HermanoEntity();
		hermano.setApellidos((String) datos.get(APELLIDOS));
		hermano.setNombre((String) datos.get(NOMBRE));
		hermano.setNumeroHermano((String) datos.get(NUMERO));
		hermano.setFechaNacimiento(fecha(datos));
		hermano.setActivo(true);
		hermano.setImportacion(importacion);
		return hermanoRepository.save(hermano);
	}

	/** El número de hermano cambia cada año: se actualiza. La fecha solo se completa si faltaba. */
	private HermanoEntity actualizarHermano(ImportacionFilaEntity fila) {
		HermanoEntity hermano = fila.getHermanoCoincidente();
		hermano.setNumeroHermano((String) fila.getDatos().get(NUMERO));
		if (hermano.getFechaNacimiento() == null) {
			hermano.setFechaNacimiento(fecha(fila.getDatos()));
		}
		hermano.setActivo(true);
		return hermano;
	}

	private static Map<String, Object> datos(FilaLeida leida) {
		Map<String, Object> datos = new HashMap<>();
		datos.put(NOMBRE_ORIGINAL, leida.nombreOriginal());
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

	private ImportacionEntity buscar(Long id) {
		// Las importaciones de túnicas tienen su propio asistente (y sus propios permisos).
		return importacionRepository.findById(id)
				.filter(i -> i.getTipo() == TipoImportacionEnum.HERMANOS)
				.orElseThrow(() -> new RecursoNoEncontradoException("la importación", id));
	}

	private static void exigirPrevisualizada(ImportacionEntity importacion) {
		if (importacion.getEstado() != EstadoImportacionEnum.PREVISUALIZADA) {
			throw new ReglaNegocioException("La importación ya está " + importacion.getEstado().name().toLowerCase());
		}
	}

}
