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
import es.nonomldev.gestiontunicas.importacion.service.ImportacionTunicasService;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelTunicas;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelTunicas.FilaLeida;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ImportacionTunicasServiceImpl implements ImportacionTunicasService {

	/** Claves de {@code datos} de cada fila (las lee el asistente del front). */
	static final String CODIGO_ORIGINAL = "codigoOriginal";
	static final String MEDIDA_ORIGINAL = "medidaOriginal";
	static final String CODIGO = "codigo";
	static final String MEDIDA = "medidaCm";
	static final String DOBLADILLO = "dobladilloCm";
	static final String MEDIDA_TOTAL = "medidaTotalCm";
	/** Aviso informativo: el código era de una túnica que ahora es propiedad de un hermano. */
	static final String CODIGO_FUE_DE_HERMANO = "codigoFueDeHermano";
	/** Túnica del inventario con el mismo código, para compararlas lado a lado. */
	static final String EXISTENTE = "existente";

	private final LectorExcelTunicas lector;
	private final ImportacionRepository importacionRepository;
	private final ImportacionFilaRepository filaRepository;
	private final TunicaRepository tunicaRepository;
	private final ImportacionMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	public ImportacionDTO previsualizar(String nombreFichero, InputStream excel) {
		List<FilaLeida> leidas = lector.leer(excel);
		if (leidas.isEmpty()) {
			throw new ReglaNegocioException("El fichero no tiene ninguna fila con datos");
		}

		ImportacionEntity importacion = new ImportacionEntity();
		importacion.setTipo(TipoImportacionEnum.TUNICAS);
		importacion.setNombreFichero(nombreFichero);
		importacion.setEstado(EstadoImportacionEnum.PREVISUALIZADA);
		importacion.setUsuario(usuarioActualService.obtener());
		importacion.setCreadoEn(Instant.now(clock));

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
				.filter(f -> f.getImportacion().getTipo() == TipoImportacionEnum.TUNICAS)
				.orElseThrow(() -> new RecursoNoEncontradoException("la fila de importación", filaId));
		exigirPrevisualizada(fila.getImportacion());
		if (fila.getEstado() == EstadoFilaEnum.ERROR) {
			throw new ReglaNegocioException("La fila %d tiene errores y no se puede importar".formatted(fila.getNumFila()));
		}
		boolean yaExiste = fila.getEstado() == EstadoFilaEnum.POSIBLE_DUPLICADO;
		if (yaExiste && request.decision() == DecisionFilaEnum.CREAR) {
			throw new ReglaNegocioException("La túnica %s ya está en el inventario: se puede actualizar o ignorar"
					.formatted(fila.getDatos().get(CODIGO)));
		}
		if (!yaExiste && request.decision() == DecisionFilaEnum.ACTUALIZAR) {
			throw new ReglaNegocioException("La túnica %s no está en el inventario: se puede crear o ignorar"
					.formatted(fila.getDatos().get(CODIGO)));
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
			throw new ReglaNegocioException("Quedan %d túnicas que ya están en el inventario sin decidir".formatted(sinDecidir));
		}

		LocalDate hoy = LocalDate.now(clock);
		for (ImportacionFilaEntity fila : importacion.getFilas()) {
			if (fila.getEstado() == EstadoFilaEnum.ERROR || fila.getDecision() == DecisionFilaEnum.IGNORAR) {
				continue;
			}
			TunicaEntity tunica = fila.getDecision() == DecisionFilaEnum.ACTUALIZAR
					? actualizarTunica(fila)
					: crearTunica(fila, importacion, hoy);
			fila.setEntidadResultanteId(tunica.getId());
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
	 * <li>Código que ya está en el inventario → POSIBLE_DUPLICADO: se actualizan sus medidas o se ignora
	 * (si las medidas son iguales, ya viene marcada para ignorar).</li>
	 * <li>Código libre → OK, se crea. Si era de una túnica que ahora es de un hermano, se avisa.</li>
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

		var existente = tunicaRepository.findEnInventarioByCodigo(leida.codigo());
		if (existente.isPresent()) {
			TunicaEntity tunica = existente.get();
			fila.getDatos().put(EXISTENTE, Map.of(
					"id", tunica.getId(),
					MEDIDA, tunica.getMedidaCm(),
					DOBLADILLO, tunica.getDobladilloCm(),
					MEDIDA_TOTAL, tunica.calcularMedidaTotalCm(),
					"estado", tunica.getEstado().name()));
			fila.setEstado(EstadoFilaEnum.POSIBLE_DUPLICADO);
			boolean mismasMedidas = tunica.getMedidaCm().equals(leida.medidaCm())
					&& tunica.getDobladilloCm().equals(leida.dobladilloCm());
			fila.setDecision(mismasMedidas ? DecisionFilaEnum.IGNORAR : null);
			return fila;
		}

		fila.getDatos().put(CODIGO_FUE_DE_HERMANO,
				tunicaRepository.existsByCodigoAndEstado(leida.codigo(), EstadoTunicaEnum.PROPIEDAD_HERMANO));
		fila.setEstado(EstadoFilaEnum.OK);
		fila.setDecision(DecisionFilaEnum.CREAR);
		return fila;
	}

	private TunicaEntity crearTunica(ImportacionFilaEntity fila, ImportacionEntity importacion, LocalDate hoy) {
		String codigo = (String) fila.getDatos().get(CODIGO);
		// Puede haberse dado de alta a mano entre la previsualización y la confirmación.
		if (tunicaRepository.existeEnInventario(codigo)) {
			throw new ReglaNegocioException(
					"La túnica %s se ha dado de alta después de subir el fichero. Vuelve a subirlo".formatted(codigo));
		}
		TunicaEntity tunica = new TunicaEntity();
		tunica.setCodigo(codigo);
		tunica.setMedidaCm(entero(fila.getDatos(), MEDIDA));
		tunica.setDobladilloCm(entero(fila.getDatos(), DOBLADILLO));
		tunica.setEstado(EstadoTunicaEnum.DISPONIBLE);
		tunica.setFechaAlta(hoy);
		tunica.setImportacion(importacion);
		tunica.crearPiezas();
		return tunicaRepository.save(tunica);
	}

	/** Solo cambian la medida y el dobladillo; el estado y las piezas se conservan. */
	private TunicaEntity actualizarTunica(ImportacionFilaEntity fila) {
		String codigo = (String) fila.getDatos().get(CODIGO);
		TunicaEntity tunica = tunicaRepository.findEnInventarioByCodigo(codigo)
				.orElseThrow(() -> new ReglaNegocioException(
						"La túnica %s ya no está en el inventario. Vuelve a subir el fichero".formatted(codigo)));
		tunica.setMedidaCm(entero(fila.getDatos(), MEDIDA));
		tunica.setDobladilloCm(entero(fila.getDatos(), DOBLADILLO));
		return tunica;
	}

	private static Map<String, Object> datos(FilaLeida leida) {
		Map<String, Object> datos = new HashMap<>();
		datos.put(CODIGO_ORIGINAL, leida.codigoOriginal());
		datos.put(MEDIDA_ORIGINAL, leida.medidaOriginal());
		datos.put(CODIGO, leida.codigo());
		datos.put(MEDIDA, leida.medidaCm());
		datos.put(DOBLADILLO, leida.dobladilloCm());
		datos.put(MEDIDA_TOTAL, leida.medidaCm() == null || leida.dobladilloCm() == null
				? null
				: leida.medidaCm() + leida.dobladilloCm());
		return datos;
	}

	/** Los números vuelven del JSON como Integer o Long según su tamaño. */
	private static Integer entero(Map<String, Object> datos, String clave) {
		return ((Number) datos.get(clave)).intValue();
	}

	private ImportacionEntity buscar(Long id) {
		return importacionRepository.findById(id)
				.filter(i -> i.getTipo() == TipoImportacionEnum.TUNICAS)
				.orElseThrow(() -> new RecursoNoEncontradoException("la importación", id));
	}

	private static void exigirPrevisualizada(ImportacionEntity importacion) {
		if (importacion.getEstado() != EstadoImportacionEnum.PREVISUALIZADA) {
			throw new ReglaNegocioException("La importación ya está " + importacion.getEstado().name().toLowerCase());
		}
	}

}
