package es.nonomldev.gestiontunicas.tunica.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.mapper.TunicaMapperImpl;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;

class TunicaServiceImplTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

	private final TunicaRepository tunicaRepository = mock(TunicaRepository.class);
	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneId.of("Europe/Madrid"));
	private final TunicaServiceImpl servicio = new TunicaServiceImpl(tunicaRepository, repartoRepository,
			new TunicaMapperImpl(), clock);

	@BeforeEach
	void preparar() {
		when(tunicaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		when(tunicaRepository.proponerPorAltura(org.mockito.ArgumentMatchers.anyInt(),
				org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
	}

	// Alta y edición

	@Test
	void alDarDeAltaCreaSusCincoPiezasDisponibleYConFechaDeHoy() {
		var creada = servicio.crear(new TunicaRequest("M-012", 120, 4, null, "Nueva"));

		assertThat(creada.codigo()).isEqualTo("M-012");
		assertThat(creada.monaguillo()).isTrue();
		assertThat(creada.estado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		assertThat(creada.fechaAlta()).isEqualTo(HOY);
		assertThat(creada.piezas()).extracting(p -> p.tipo()).containsExactly(TipoPiezaEnum.values());
		assertThat(creada.piezas()).allSatisfy(p -> assertThat(p.estado()).isEqualTo(EstadoPiezaEnum.BUEN_ESTADO));
	}

	@Test
	void respetaLaFechaDeAltaSiSeIndica() {
		assertThat(servicio.crear(new TunicaRequest("42", 150, 0, LocalDate.of(2020, 3, 1), null)).fechaAlta())
				.isEqualTo(LocalDate.of(2020, 3, 1));
	}

	@Test
	void noSeDaDeAltaUnCodigoQueYaEstaEnElInventario() {
		when(tunicaRepository.existeEnInventario("42")).thenReturn(true);

		assertThatThrownBy(() -> servicio.crear(new TunicaRequest("42", 150, 0, null, null)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("Ya hay una túnica en el inventario con el código 42");
		verify(tunicaRepository, never()).save(any());
	}

	@Test
	void alEditarConservaSuPropioCodigoSinComprobarlo() {
		TunicaEntity tunica = tunica("42", EstadoTunicaEnum.EN_REPARACION);
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica));

		var editada = servicio.actualizar(7L, new TunicaRequest("42", 160, 8, null, "Dobladillo nuevo"));

		assertThat(editada.dobladilloCm()).isEqualTo(8);
		// El estado y la fecha de alta no se tocan al editar.
		assertThat(editada.estado()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);
		assertThat(editada.fechaAlta()).isEqualTo(LocalDate.of(2025, 1, 10));
		verify(tunicaRepository, never()).existeEnInventario(anyString());
	}

	@Test
	void alEditarNoPuedeQuedarseConElCodigoDeOtra() {
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica("42", EstadoTunicaEnum.DISPONIBLE)));
		when(tunicaRepository.existeEnInventario("43")).thenReturn(true);

		assertThatThrownBy(() -> servicio.actualizar(7L, new TunicaRequest("43", 160, 0, null, null)))
				.hasMessage("Ya hay una túnica en el inventario con el código 43");
	}

	@Test
	void unaTunicaQueEsDeUnHermanoNoSeEdita() {
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica("42", EstadoTunicaEnum.PROPIEDAD_HERMANO)));

		assertThatThrownBy(() -> servicio.actualizar(7L, new TunicaRequest("42", 160, 0, null, null)))
				.hasMessage("La túnica 42 es propiedad de un hermano y no se puede modificar");
	}

	// Baja

	@ParameterizedTest(name = "con la túnica {0} se puede dar de baja")
	@EnumSource(value = EstadoTunicaEnum.class, names = { "DISPONIBLE", "EN_REVISION", "EN_REPARACION", "INCOMPLETA" })
	void seDaDeBajaLaQueEstaEnCasa(EstadoTunicaEnum estado) {
		TunicaEntity tunica = tunica("42", estado);
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica));

		servicio.darDeBaja(7L, new MotivoRequest("Rota sin arreglo"));

		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.BAJA);
		assertThat(tunica.getFechaBaja()).isEqualTo(HOY);
		assertThat(tunica.getMotivoBaja()).isEqualTo("Rota sin arreglo");
	}

	@ParameterizedTest(name = "con la túnica {0} no se puede dar de baja")
	@EnumSource(value = EstadoTunicaEnum.class, names = { "RESERVADA", "ALQUILADA", "NO_DEVUELTA", "PROPIEDAD_HERMANO" })
	void noSeDaDeBajaLaQueEstaFuera(EstadoTunicaEnum estado) {
		TunicaEntity tunica = tunica("42", estado);
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica));

		assertThatThrownBy(() -> servicio.darDeBaja(7L, new MotivoRequest("Rota")))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageStartingWith("La túnica 42 no se puede dar de baja: está ");
		assertThat(tunica.getEstado()).isEqualTo(estado);
	}

	// Propuesta: de dónde sale la tolerancia

	@Test
	void laToleranciaIndicadaManda() {
		servicio.proponer(164, 8);

		verify(tunicaRepository).proponerPorAltura(164, 8);
		verify(repartoRepository, never()).findFirstByEstado(any());
	}

	@Test
	void sinToleranciaSeUsaLaDelRepartoAbierto() {
		RepartoEntity reparto = new RepartoEntity();
		reparto.setToleranciaMedidaCm(5);
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.of(reparto));

		servicio.proponer(164, null);

		verify(tunicaRepository).proponerPorAltura(164, 5);
	}

	@Test
	void sinRepartoAbiertoSeUsanTresCentimetros() {
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.empty());

		servicio.proponer(164, null);

		verify(tunicaRepository).proponerPorAltura(164, 3);
	}

	@Test
	void laToleranciaCeroEsValidaYNoSeConfundeConNoIndicarla() {
		servicio.proponer(164, 0);

		verify(tunicaRepository).proponerPorAltura(164, 0);
		verify(repartoRepository, never()).findFirstByEstado(any());
	}

	// Inventario

	@Test
	void sinFiltroDeEstadoSalenTodasMenosLasDeHermanos() {
		when(tunicaRepository.buscar(any(), any(), any(), any())).thenReturn(Page.empty());

		servicio.buscar(Set.of(), null, null, PageRequest.of(0, 25));
		servicio.buscar(EnumSet.of(EstadoTunicaEnum.PROPIEDAD_HERMANO), 150, 160, PageRequest.of(0, 25));

		verify(tunicaRepository).buscar(eq(EnumSet.complementOf(EnumSet.of(EstadoTunicaEnum.PROPIEDAD_HERMANO))),
				eq(null), eq(null), any());
		verify(tunicaRepository).buscar(eq(EnumSet.of(EstadoTunicaEnum.PROPIEDAD_HERMANO)), eq(150), eq(160), any());
	}

	// Reglas de la entidad

	@Test
	void elEstadoSegunSusPiezasDaPrioridadALaPerdida() {
		TunicaEntity tunica = tunica("42", EstadoTunicaEnum.EN_REVISION);
		assertThat(tunica.estadoSegunPiezas()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);

		pieza(tunica, TipoPiezaEnum.CAPA).setEstado(EstadoPiezaEnum.DETERIORADA);
		assertThat(tunica.estadoSegunPiezas()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);

		pieza(tunica, TipoPiezaEnum.TALEGA).setEstado(EstadoPiezaEnum.PERDIDA);
		assertThat(tunica.estadoSegunPiezas()).isEqualTo(EstadoTunicaEnum.INCOMPLETA);
	}

	@Test
	void encajaConLaAlturaDentroDeLaToleranciaIncluida() {
		TunicaEntity tunica = tunica("42", EstadoTunicaEnum.DISPONIBLE); // 158 + 6 = 164

		assertThat(tunica.encajaCon(167, 3)).isTrue();
		assertThat(tunica.encajaCon(161, 3)).isTrue();
		assertThat(tunica.encajaCon(168, 3)).isFalse();
		assertThat(tunica.encajaCon(164, 0)).isTrue();
		assertThat(tunica.encajaCon(165, 0)).isFalse();
	}

	private static TunicaEntity tunica(String codigo, EstadoTunicaEnum estado) {
		TunicaEntity tunica = new TunicaEntity();
		ReflectionTestUtils.setField(tunica, "id", 7L);
		tunica.setCodigo(codigo);
		tunica.setMedidaCm(158);
		tunica.setDobladilloCm(6);
		tunica.setEstado(estado);
		tunica.setFechaAlta(LocalDate.of(2025, 1, 10));
		tunica.crearPiezas();
		return tunica;
	}

	private static PiezaEntity pieza(TunicaEntity tunica, TipoPiezaEnum tipo) {
		return tunica.pieza(tipo).orElseThrow();
	}

}
