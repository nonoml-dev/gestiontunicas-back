package es.nonomldev.gestiontunicas.importacion.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.entity.DecisionFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionEntity;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFilaEntity;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.mapper.ImportacionMapper;
import es.nonomldev.gestiontunicas.importacion.repository.ImportacionFilaRepository;
import es.nonomldev.gestiontunicas.importacion.repository.ImportacionRepository;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelTunicas;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelTunicas.FilaLeida;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;

class ImportacionTunicasServiceImplTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 24);
	private static final InputStream EXCEL = InputStream.nullInputStream();

	private final LectorExcelTunicas lector = mock(LectorExcelTunicas.class);
	private final ImportacionRepository importacionRepository = mock(ImportacionRepository.class);
	private final ImportacionFilaRepository filaRepository = mock(ImportacionFilaRepository.class);
	private final TunicaRepository tunicaRepository = mock(TunicaRepository.class);
	private final ImportacionMapper mapper = mock(ImportacionMapper.class);
	private final UsuarioActualService usuarioActual = mock(UsuarioActualService.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneId.of("Europe/Madrid"));

	private final ImportacionTunicasServiceImpl servicio = new ImportacionTunicasServiceImpl(lector,
			importacionRepository, filaRepository, tunicaRepository, mapper, usuarioActual, clock);

	/** Última importación guardada al previsualizar. */
	private ImportacionEntity guardada;

	@BeforeEach
	void preparar() {
		when(usuarioActual.obtener()).thenReturn(new UsuarioEntity());
		when(importacionRepository.save(any())).thenAnswer(invocacion -> {
			guardada = invocacion.getArgument(0);
			return guardada;
		});
		when(tunicaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
		when(tunicaRepository.findEnInventarioByCodigo(anyString())).thenReturn(Optional.empty());
	}

	@Test
	void clasificaLasFilasSinTocarElInventario() {
		when(tunicaRepository.findEnInventarioByCodigo("2")).thenReturn(Optional.of(tunica("2", 115, 10)));
		when(tunicaRepository.findEnInventarioByCodigo("3")).thenReturn(Optional.of(tunica("3", 130, 5)));
		when(tunicaRepository.existsByCodigoAndEstado("4", EstadoTunicaEnum.PROPIEDAD_HERMANO)).thenReturn(true);
		when(lector.leer(EXCEL)).thenReturn(List.of(
				leida(2, "1", 140, 0),
				leida(3, "2", 115, 10), // ya existe con las mismas medidas
				leida(4, "3", 135, 5), // ya existe con otras medidas
				leida(5, "4", 150, 2), // su código fue de una túnica que ahora es de un hermano
				new FilaLeida(6, "248", "", "248", null, null, List.of("Falta la medida"))));

		servicio.previsualizar("inventario.xlsx", EXCEL);

		assertThat(guardada.getTipo()).isEqualTo(TipoImportacionEnum.TUNICAS);
		assertThat(guardada.getEstado()).isEqualTo(EstadoImportacionEnum.PREVISUALIZADA);
		assertThat(guardada.getFilas()).extracting(ImportacionFilaEntity::getEstado, ImportacionFilaEntity::getDecision)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(EstadoFilaEnum.OK, DecisionFilaEnum.CREAR),
						org.assertj.core.groups.Tuple.tuple(EstadoFilaEnum.POSIBLE_DUPLICADO, DecisionFilaEnum.IGNORAR),
						org.assertj.core.groups.Tuple.tuple(EstadoFilaEnum.POSIBLE_DUPLICADO, null),
						org.assertj.core.groups.Tuple.tuple(EstadoFilaEnum.OK, DecisionFilaEnum.CREAR),
						org.assertj.core.groups.Tuple.tuple(EstadoFilaEnum.ERROR, null));
		assertThat(guardada.getFilas().get(0).getDatos()).containsEntry("medidaTotalCm", 140)
				.containsEntry("codigoFueDeHermano", false);
		assertThat(guardada.getFilas().get(2).getDatos().get("existente"))
				.isEqualTo(Map.of("id", 30L, "medidaCm", 130, "dobladilloCm", 5, "medidaTotalCm", 135, "estado", "DISPONIBLE"));
		assertThat(guardada.getFilas().get(3).getDatos()).containsEntry("codigoFueDeHermano", true);
		assertThat(guardada.getFilas().get(4).getErrores()).containsExactly("Falta la medida");
		assertThat(guardada.getTotalFilas()).isEqualTo(5);
		assertThat(guardada.getFilasOk()).isEqualTo(2);
		assertThat(guardada.getFilasError()).isEqualTo(1);
		assertThat(guardada.getFilasDuplicadas()).isEqualTo(2);
		verify(tunicaRepository, never()).save(any());
	}

	@Test
	void unFicheroSinFilasNoCreaLaImportacion() {
		when(lector.leer(EXCEL)).thenReturn(List.of());

		assertThatThrownBy(() -> servicio.previsualizar("vacio.xlsx", EXCEL))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("El fichero no tiene ninguna fila con datos");
	}

	@Test
	void alConfirmarCreaLasNuevasConSusPiezasActualizaLasDecididasYSaltaErroresEIgnoradas() {
		TunicaEntity existente = tunica("3", 130, 5);
		existente.setEstado(EstadoTunicaEnum.EN_REPARACION);
		when(tunicaRepository.findEnInventarioByCodigo("2")).thenReturn(Optional.of(tunica("2", 115, 10)));
		when(tunicaRepository.findEnInventarioByCodigo("3")).thenReturn(Optional.of(existente));
		when(lector.leer(EXCEL)).thenReturn(List.of(
				leida(2, "1", 140, 0),
				leida(3, "2", 115, 10),
				leida(4, "3", 135, 5),
				new FilaLeida(5, "248", "", "248", null, null, List.of("Falta la medida"))));
		servicio.previsualizar("inventario.xlsx", EXCEL);
		ImportacionEntity importacion = conId(guardada, 9L);
		ImportacionFilaEntity duplicada = importacion.getFilas().get(2);
		when(filaRepository.findById(44L)).thenReturn(Optional.of(duplicada));

		assertThatThrownBy(() -> servicio.confirmar(9L))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("Quedan 1 túnicas que ya están en el inventario sin decidir");

		servicio.decidir(44L, new DecisionFilaRequest(DecisionFilaEnum.ACTUALIZAR));
		servicio.confirmar(9L);

		var creadas = org.mockito.ArgumentCaptor.forClass(TunicaEntity.class);
		verify(tunicaRepository).save(creadas.capture());
		TunicaEntity nueva = creadas.getValue();
		assertThat(nueva.getCodigo()).isEqualTo("1");
		assertThat(nueva.getMedidaCm()).isEqualTo(140);
		assertThat(nueva.getDobladilloCm()).isZero();
		assertThat(nueva.getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		assertThat(nueva.getFechaAlta()).isEqualTo(HOY);
		assertThat(nueva.getImportacion()).isSameAs(importacion);
		assertThat(nueva.getPiezas()).extracting(PiezaEntity::getTipo).containsExactly(TipoPiezaEnum.values());

		// Se actualizan solo las medidas: el estado se conserva.
		assertThat(existente.getMedidaCm()).isEqualTo(135);
		assertThat(existente.getEstado()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);
		assertThat(duplicada.getEntidadResultanteId()).isEqualTo(30L);
		assertThat(importacion.getEstado()).isEqualTo(EstadoImportacionEnum.CONFIRMADA);
		assertThat(importacion.getConfirmadoEn()).isEqualTo(Instant.now(clock));
	}

	@Test
	void siElCodigoSeDioDeAltaDespuesDeSubirElFicheroNoSeImportaNada() {
		when(lector.leer(EXCEL)).thenReturn(List.of(leida(2, "1", 140, 0)));
		servicio.previsualizar("inventario.xlsx", EXCEL);
		conId(guardada, 9L);
		when(tunicaRepository.existeEnInventario("1")).thenReturn(true);

		assertThatThrownBy(() -> servicio.confirmar(9L))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("La túnica 1 se ha dado de alta después de subir el fichero. Vuelve a subirlo");
		verify(tunicaRepository, never()).save(any());
	}

	@Test
	void unaTunicaQueYaExisteNoSePuedeCrearNiUnaNuevaActualizar() {
		when(tunicaRepository.findEnInventarioByCodigo("2")).thenReturn(Optional.of(tunica("2", 115, 10)));
		when(lector.leer(EXCEL)).thenReturn(List.of(leida(2, "1", 140, 0), leida(3, "2", 120, 10),
				new FilaLeida(4, "x", "", null, null, null, List.of("Falta la medida"))));
		servicio.previsualizar("inventario.xlsx", EXCEL);
		conId(guardada, 9L);
		when(filaRepository.findById(1L)).thenReturn(Optional.of(guardada.getFilas().get(0)));
		when(filaRepository.findById(2L)).thenReturn(Optional.of(guardada.getFilas().get(1)));
		when(filaRepository.findById(3L)).thenReturn(Optional.of(guardada.getFilas().get(2)));

		assertThatThrownBy(() -> servicio.decidir(2L, new DecisionFilaRequest(DecisionFilaEnum.CREAR)))
				.hasMessage("La túnica 2 ya está en el inventario: se puede actualizar o ignorar");
		assertThatThrownBy(() -> servicio.decidir(1L, new DecisionFilaRequest(DecisionFilaEnum.ACTUALIZAR)))
				.hasMessage("La túnica 1 no está en el inventario: se puede crear o ignorar");
		assertThatThrownBy(() -> servicio.decidir(3L, new DecisionFilaRequest(DecisionFilaEnum.IGNORAR)))
				.hasMessage("La fila 4 tiene errores y no se puede importar");

		servicio.decidir(1L, new DecisionFilaRequest(DecisionFilaEnum.IGNORAR));
		assertThat(guardada.getFilas().get(0).getDecision()).isEqualTo(DecisionFilaEnum.IGNORAR);
	}

	@Test
	void unaImportacionConfirmadaOCanceladaNoSeVuelveATocar() {
		when(lector.leer(EXCEL)).thenReturn(List.of(leida(2, "1", 140, 0)));
		servicio.previsualizar("inventario.xlsx", EXCEL);
		conId(guardada, 9L);

		servicio.cancelar(9L);

		assertThat(guardada.getEstado()).isEqualTo(EstadoImportacionEnum.CANCELADA);
		assertThatThrownBy(() -> servicio.confirmar(9L)).hasMessage("La importación ya está cancelada");
		verify(tunicaRepository, never()).save(any());
	}

	@Test
	void noDaAccesoAImportacionesDeHermanos() {
		ImportacionEntity deHermanos = new ImportacionEntity();
		deHermanos.setTipo(TipoImportacionEnum.HERMANOS);
		when(importacionRepository.findById(5L)).thenReturn(Optional.of(deHermanos));
		ImportacionFilaEntity fila = new ImportacionFilaEntity();
		deHermanos.addFila(fila);
		when(filaRepository.findById(6L)).thenReturn(Optional.of(fila));

		assertThatThrownBy(() -> servicio.confirmar(5L)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio.filas(5L, null)).isInstanceOf(RecursoNoEncontradoException.class);
		assertThatThrownBy(() -> servicio.decidir(6L, new DecisionFilaRequest(DecisionFilaEnum.IGNORAR)))
				.isInstanceOf(RecursoNoEncontradoException.class);
		verify(mapper, never()).toDTO(eq(deHermanos));
	}

	private ImportacionEntity conId(ImportacionEntity importacion, Long id) {
		ReflectionTestUtils.setField(importacion, "id", id);
		when(importacionRepository.findById(id)).thenReturn(Optional.of(importacion));
		return importacion;
	}

	private static FilaLeida leida(int numFila, String codigo, int medida, int dobladillo) {
		return new FilaLeida(numFila, codigo, medida + "+" + dobladillo, codigo, medida, dobladillo, new ArrayList<>());
	}

	private static TunicaEntity tunica(String codigo, int medida, int dobladillo) {
		TunicaEntity tunica = new TunicaEntity();
		ReflectionTestUtils.setField(tunica, "id", Long.parseLong(codigo) * 10);
		tunica.setCodigo(codigo);
		tunica.setMedidaCm(medida);
		tunica.setDobladilloCm(dobladillo);
		tunica.setEstado(EstadoTunicaEnum.DISPONIBLE);
		return tunica;
	}

}
