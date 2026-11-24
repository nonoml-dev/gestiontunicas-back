package es.nonomldev.gestiontunicas.tunica.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.ReparacionEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.mapper.TunicaMapperImpl;
import es.nonomldev.gestiontunicas.tunica.repository.PiezaRepository;
import es.nonomldev.gestiontunicas.tunica.repository.ReparacionRepository;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;

class ReparacionServiceImplTest {

	private static final LocalDate INICIO = LocalDate.of(2026, 11, 24);

	private final ReparacionRepository reparacionRepository = mock(ReparacionRepository.class);
	private final PiezaRepository piezaRepository = mock(PiezaRepository.class);
	private final TunicaRepository tunicaRepository = mock(TunicaRepository.class);
	private final ReparacionServiceImpl servicio = new ReparacionServiceImpl(reparacionRepository, piezaRepository,
			tunicaRepository, new TunicaMapperImpl());

	private TunicaEntity tunica;

	@BeforeEach
	void preparar() {
		tunica = new TunicaEntity();
		ReflectionTestUtils.setField(tunica, "id", 7L);
		tunica.setCodigo("42");
		tunica.setEstado(EstadoTunicaEnum.DISPONIBLE);
		tunica.crearPiezas();
		long id = 70;
		for (PiezaEntity pieza : tunica.getPiezas()) {
			ReflectionTestUtils.setField(pieza, "id", id++);
		}
		when(tunicaRepository.findById(7L)).thenReturn(Optional.of(tunica));
		when(tunicaRepository.existsById(7L)).thenReturn(true);
		when(piezaRepository.findByTunicaIdAndTipo(eq(7L), any()))
				.thenAnswer(i -> tunica.pieza(i.getArgument(1)));
		when(reparacionRepository.save(any())).thenAnswer(i -> {
			ReparacionEntity reparacion = i.getArgument(0);
			ReflectionTestUtils.setField(reparacion, "id", 500L);
			return reparacion;
		});
	}

	@Test
	void alRegistrarlaLaPiezaYLaTunicaPasanAEnReparacion() {
		var dto = servicio.registrar(7L, peticion(TipoPiezaEnum.CAPA, null, null));

		assertThat(dto.estado()).isEqualTo(EstadoReparacionEnum.PENDIENTE);
		assertThat(dto.tipoPieza()).isEqualTo(TipoPiezaEnum.CAPA);
		assertThat(pieza(TipoPiezaEnum.CAPA).getEstado()).isEqualTo(EstadoPiezaEnum.EN_REPARACION);
		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);
	}

	@Test
	void alTerminarLaUltimaLaTunicaVuelveAEstarDisponible() {
		ReparacionEntity reparacion = abierta(TipoPiezaEnum.CAPA);

		var dto = servicio.actualizar(7L, 500L,
				peticion(TipoPiezaEnum.CAPA, EstadoReparacionEnum.FINALIZADA, INICIO.plusDays(3)));

		assertThat(dto.estado()).isEqualTo(EstadoReparacionEnum.FINALIZADA);
		assertThat(dto.fechaFin()).isEqualTo(INICIO.plusDays(3));
		assertThat(reparacion.getPieza().getEstado()).isEqualTo(EstadoPiezaEnum.BUEN_ESTADO);
		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
	}

	@Test
	void siQuedaOtraPiezaPerdidaLaTunicaQuedaIncompleta() {
		abierta(TipoPiezaEnum.CAPA);
		pieza(TipoPiezaEnum.CINGULO).setEstado(EstadoPiezaEnum.PERDIDA);

		servicio.actualizar(7L, 500L, peticion(TipoPiezaEnum.CAPA, EstadoReparacionEnum.FINALIZADA, INICIO));

		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.INCOMPLETA);
	}

	@Test
	void conOtraReparacionAbiertaDeLaMismaPiezaSigueEnReparacion() {
		abierta(TipoPiezaEnum.CAPA);
		when(reparacionRepository.existsByPiezaIdAndEstadoIn(eq(pieza(TipoPiezaEnum.CAPA).getId()), any()))
				.thenReturn(true);

		servicio.actualizar(7L, 500L, peticion(TipoPiezaEnum.CAPA, EstadoReparacionEnum.FINALIZADA, INICIO));

		assertThat(pieza(TipoPiezaEnum.CAPA).getEstado()).isEqualTo(EstadoPiezaEnum.EN_REPARACION);
		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);
	}

	@Test
	void reponerUnaPiezaPerdidaDejaLaTunicaEnReparacionHastaTerminar() {
		pieza(TipoPiezaEnum.TALEGA).setEstado(EstadoPiezaEnum.PERDIDA);
		tunica.setEstado(EstadoTunicaEnum.INCOMPLETA);

		servicio.registrar(7L, new ReparacionRequest(TipoPiezaEnum.TALEGA, TipoReparacionEnum.REPOSICION,
				"Talega nueva", null, INICIO, null, null));

		assertThat(pieza(TipoPiezaEnum.TALEGA).getEstado()).isEqualTo(EstadoPiezaEnum.EN_REPARACION);
		assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.EN_REPARACION);
	}

	@ParameterizedTest
	@EnumSource(value = EstadoTunicaEnum.class, names = { "ALQUILADA", "RESERVADA", "NO_DEVUELTA" })
	void siLaTunicaEstaFueraSeRegistraSinCambiarSuEstado(EstadoTunicaEnum fuera) {
		tunica.setEstado(fuera);

		servicio.registrar(7L, peticion(TipoPiezaEnum.ANTIFAZ, null, null));

		assertThat(pieza(TipoPiezaEnum.ANTIFAZ).getEstado()).isEqualTo(EstadoPiezaEnum.EN_REPARACION);
		assertThat(tunica.getEstado()).isEqualTo(fuera);
	}

	@Test
	void noSeReparanTunicasDadasDeBajaNiDeUnHermano() {
		tunica.setEstado(EstadoTunicaEnum.BAJA);
		assertThatThrownBy(() -> servicio.registrar(7L, peticion(TipoPiezaEnum.CAPA, null, null)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("La túnica 42 está dada de baja: no se le registran reparaciones");

		tunica.setEstado(EstadoTunicaEnum.PROPIEDAD_HERMANO);
		assertThatThrownBy(() -> servicio.registrar(7L, peticion(TipoPiezaEnum.CAPA, null, null)))
				.hasMessage("La túnica 42 es propiedad de un hermano: no se le registran reparaciones");
		verify(reparacionRepository, never()).save(any());
	}

	@Test
	void validaLasFechas() {
		assertThatThrownBy(() -> servicio.registrar(7L,
				peticion(TipoPiezaEnum.CAPA, EstadoReparacionEnum.FINALIZADA, null)))
				.hasMessage("Indica la fecha en que se terminó la reparación");
		assertThatThrownBy(() -> servicio.registrar(7L,
				peticion(TipoPiezaEnum.CAPA, EstadoReparacionEnum.FINALIZADA, INICIO.minusDays(1))))
				.hasMessage("La fecha de fin no puede ser anterior a la de inicio");
	}

	@Test
	void noSeCambiaLaPiezaNiSeTocaUnaReparacionDeOtraTunica() {
		abierta(TipoPiezaEnum.CAPA);

		assertThatThrownBy(() -> servicio.actualizar(7L, 500L, peticion(TipoPiezaEnum.ANTIFAZ, null, null)))
				.hasMessage("No se puede cambiar la pieza de una reparación ya registrada");
		assertThatThrownBy(() -> servicio.actualizar(8L, 500L, peticion(TipoPiezaEnum.CAPA, null, null)))
				.isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void laListaDeUnaTunicaQueNoExisteEsUn404() {
		when(tunicaRepository.existsById(anyLong())).thenReturn(false);

		assertThatThrownBy(() -> servicio.deTunica(99L)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	/** Reparación abierta (id 500) de esa pieza, con la túnica en reparación. */
	private ReparacionEntity abierta(TipoPiezaEnum tipo) {
		ReparacionEntity reparacion = new ReparacionEntity();
		ReflectionTestUtils.setField(reparacion, "id", 500L);
		reparacion.setPieza(pieza(tipo));
		reparacion.setTipo(TipoReparacionEnum.REPARACION);
		reparacion.setDescripcion("Escudo JHS despegado");
		reparacion.setEstado(EstadoReparacionEnum.PENDIENTE);
		reparacion.setFechaInicio(INICIO);
		pieza(tipo).setEstado(EstadoPiezaEnum.EN_REPARACION);
		tunica.setEstado(EstadoTunicaEnum.EN_REPARACION);
		when(reparacionRepository.findById(500L)).thenReturn(Optional.of(reparacion));
		return reparacion;
	}

	private PiezaEntity pieza(TipoPiezaEnum tipo) {
		return tunica.pieza(tipo).orElseThrow();
	}

	private static ReparacionRequest peticion(TipoPiezaEnum pieza, EstadoReparacionEnum estado, LocalDate fin) {
		return new ReparacionRequest(pieza, TipoReparacionEnum.REPARACION, "Escudo JHS despegado", estado, INICIO,
				fin, null);
	}

}
