package es.nonomldev.gestiontunicas.reparto.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaDTO;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapperImpl;
import es.nonomldev.gestiontunicas.reparto.repository.CambioPeriodoDevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;

/** Jornadas extraordinarias (REP-05): numeración, limosna propia o la del reparto, y casos no permitidos. */
class RepartoServiceImplJornadaTest {

	private static final LocalDate DIA_1 = LocalDate.of(2027, 3, 8);

	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private final JornadaRepository jornadaRepository = mock(JornadaRepository.class);
	private final RepartoServiceImpl servicio = new RepartoServiceImpl(repartoRepository, jornadaRepository,
			mock(ReservaRepository.class), mock(CambioPeriodoDevolucionRepository.class),
			mock(PlantillaResguardoRepository.class), mock(AlquilerRepository.class), mock(RequisitosAlquiler.class),
			new RepartoMapperImpl(), mock(UsuarioActualService.class), Clock.systemDefaultZone());

	private RepartoEntity reparto;

	@BeforeEach
	void preparar() {
		reparto = new RepartoEntity();
		ReflectionTestUtils.setField(reparto, "id", 7L);
		reparto.setAnio(2027);
		reparto.setEstado(EstadoRepartoEnum.ABIERTO);
		reparto.setLimosna(new BigDecimal("20.00"));
		reparto.setPeriodoDevolucion(new PeriodoDevolucionEntity(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 4, 30), 2));
		for (int i = 0; i < 3; i++) {
			JornadaEntity jornada = new JornadaEntity();
			jornada.setNumero(i + 1);
			jornada.setFecha(DIA_1.plusDays(i));
			reparto.addJornada(jornada);
		}
		when(repartoRepository.findWithJornadasById(7L)).thenReturn(Optional.of(reparto));
		when(jornadaRepository.maxNumero(7L)).thenReturn(3);
	}

	@Test
	void conLimosnaPropiaLaGuardaYSeNumeraTrasLaUltima() {
		RepartoDTO dto = servicio.anadirJornadaExtraordinaria(7L, jornada(LocalDate.of(2027, 3, 15), "15.50"));

		JornadaDTO extraordinaria = dto.jornadas().get(3);
		assertThat(extraordinaria.numero()).isEqualTo(4);
		assertThat(extraordinaria.extraordinaria()).isTrue();
		assertThat(extraordinaria.limosna()).isEqualByComparingTo("15.50");
		assertThat(extraordinaria.horaInicio()).isEqualTo(LocalTime.of(18, 0));
		assertThat(reparto.getJornadas().get(3).limosnaAplicable(reparto.getLimosna())).isEqualByComparingTo("15.50");
	}

	@Test
	void sinLimosnaSeAplicaLaDelReparto() {
		servicio.anadirJornadaExtraordinaria(7L, jornada(LocalDate.of(2027, 3, 15), null));

		JornadaEntity extraordinaria = reparto.getJornadas().get(3);
		assertThat(extraordinaria.getLimosna()).isNull();
		assertThat(extraordinaria.limosnaAplicable(reparto.getLimosna())).isEqualByComparingTo("20.00");
	}

	@Test
	void unaJornadaOrdinariaAplicaSiempreLaDelReparto() {
		assertThat(reparto.getJornadas().getFirst().limosnaAplicable(reparto.getLimosna())).isEqualByComparingTo("20.00");
	}

	@Test
	void noAdmiteDosJornadasElMismoDia() {
		assertThatThrownBy(() -> servicio.anadirJornadaExtraordinaria(7L, jornada(DIA_1, "15")))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("Ya hay una jornada el 08/03/2027");
		assertThat(reparto.getJornadas()).hasSize(3);
	}

	@Test
	void pideAlMenosUnaOperacion() {
		JornadaRequest sinOperaciones = new JornadaRequest(LocalDate.of(2027, 3, 15), null, null, null, false, false, false);

		assertThatThrownBy(() -> servicio.anadirJornadaExtraordinaria(7L, sinOperaciones))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("al menos una operación");
		assertThat(reparto.getJornadas()).hasSize(3);
	}

	@Test
	void unRepartoCerradoNoAdmiteJornadas() {
		reparto.setEstado(EstadoRepartoEnum.CERRADO);

		assertThatThrownBy(() -> servicio.anadirJornadaExtraordinaria(7L, jornada(LocalDate.of(2027, 3, 15), "15")))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("cerrado");
	}

	@ParameterizedTest
	@EnumSource(value = EstadoRepartoEnum.class, names = { "BORRADOR", "ABIERTO" })
	void enBorradorYAbiertoSiSeAdmite(EstadoRepartoEnum estado) {
		reparto.setEstado(estado);

		assertThat(servicio.anadirJornadaExtraordinaria(7L, jornada(LocalDate.of(2027, 3, 15), "15")).jornadas())
				.hasSize(4);
	}

	@Test
	void elHorarioTieneQueSerCoherente() {
		assertThat(new JornadaRequest(DIA_1, LocalTime.of(21, 0), LocalTime.of(18, 0), null, true, false, false)
				.isHorarioCoherente()).isFalse();
		assertThat(new JornadaRequest(DIA_1, LocalTime.of(18, 0), LocalTime.of(21, 0), null, true, false, false)
				.isHorarioCoherente()).isTrue();
		assertThat(new JornadaRequest(DIA_1, null, LocalTime.of(21, 0), null, true, false, false)
				.isHorarioCoherente()).isTrue();
	}

	private static JornadaRequest jornada(LocalDate fecha, String limosna) {
		return new JornadaRequest(fecha, LocalTime.of(18, 0), LocalTime.of(21, 0),
				limosna == null ? null : new BigDecimal(limosna), true, true, true);
	}

}
