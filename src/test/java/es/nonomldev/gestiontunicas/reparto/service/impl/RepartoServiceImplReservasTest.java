package es.nonomldev.gestiontunicas.reparto.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapperImpl;
import es.nonomldev.gestiontunicas.reparto.repository.CambioPeriodoDevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;

/** REP-06: al empezar el día 3 las reservas no recogidas se liberan solas, y una sola vez. */
class RepartoServiceImplReservasTest {

	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
	private static final LocalDate DIA_1 = LocalDate.of(2027, 3, 8);
	private static final LocalDate DIA_3 = DIA_1.plusDays(2);

	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private final ReservaRepository reservaRepository = mock(ReservaRepository.class);

	private RepartoEntity reparto;
	private ReservaEntity noRecogida;
	private TunicaEntity tunicaReservada;
	private TunicaEntity tunicaYaEntregada;

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

		tunicaReservada = tunica(42L, EstadoTunicaEnum.RESERVADA);
		noRecogida = reserva(tunicaReservada);
		// La reserva sigue activa, pero su túnica ya no está reservada (p. ej. se dio de baja): no se toca
		tunicaYaEntregada = tunica(43L, EstadoTunicaEnum.BAJA);
		ReservaEntity otra = reserva(tunicaYaEntregada);

		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.of(reparto));
		when(repartoRepository.findWithJornadasById(7L)).thenReturn(Optional.of(reparto));
		when(reservaRepository.findByRepartoIdAndEstado(7L, EstadoReservaEnum.ACTIVA)).thenReturn(List.of(noRecogida, otra));
	}

	@Test
	void antesDelDia3NoLiberaNada() {
		Optional<?> resultado = servicio(DIA_3.minusDays(1).atTime(23, 59)).liberarReservasSiEmpiezaElDia3();

		assertThat(resultado).isEmpty();
		assertThat(noRecogida.getEstado()).isEqualTo(EstadoReservaEnum.ACTIVA);
		assertThat(tunicaReservada.getEstado()).isEqualTo(EstadoTunicaEnum.RESERVADA);
		assertThat(reparto.getReservasLiberadasEn()).isNull();
	}

	@Test
	void alEmpezarElDia3LiberaLasReservasYSusTunicasQuedanDisponibles() {
		LocalDateTime medianoche = DIA_3.atStartOfDay();

		assertThat(servicio(medianoche).liberarReservasSiEmpiezaElDia3()).isPresent();

		assertThat(noRecogida.getEstado()).isEqualTo(EstadoReservaEnum.LIBERADA);
		assertThat(noRecogida.getResueltaEn()).isEqualTo(instante(medianoche));
		assertThat(tunicaReservada.getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		assertThat(tunicaYaEntregada.getEstado()).isEqualTo(EstadoTunicaEnum.BAJA);
		assertThat(reparto.getReservasLiberadasEn()).isEqualTo(instante(medianoche));
	}

	@Test
	void siElEquipoEstabaApagadoLasLiberaAlEncenderseDespues() {
		assertThat(servicio(DIA_3.plusDays(1).atTime(9, 30)).liberarReservasSiEmpiezaElDia3()).isPresent();

		assertThat(noRecogida.getEstado()).isEqualTo(EstadoReservaEnum.LIBERADA);
	}

	@Test
	void noSeDuplica_siYaSeLiberaronNoVuelveAHacerNada() {
		Instant primeraVez = instante(DIA_3.atStartOfDay());
		reparto.setReservasLiberadasEn(primeraVez);

		assertThat(servicio(DIA_3.atTime(10, 0)).liberarReservasSiEmpiezaElDia3()).isEmpty();

		verify(reservaRepository, never()).findByRepartoIdAndEstado(anyLong(), any());
		assertThat(reparto.getReservasLiberadasEn()).isEqualTo(primeraVez);
	}

	@Test
	void dosComprobacionesSeguidasSoloLiberanLaPrimera() {
		var servicio = servicio(DIA_3.atTime(0, 15));

		assertThat(servicio.liberarReservasSiEmpiezaElDia3()).isPresent();
		assertThat(servicio.liberarReservasSiEmpiezaElDia3()).isEmpty();
	}

	@Test
	void sinRepartoAbiertoNoHaceNada() {
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.empty());

		assertThat(servicio(DIA_3.atTime(10, 0)).liberarReservasSiEmpiezaElDia3()).isEmpty();
	}

	@Test
	void unaJornadaExtraordinariaNoCuentaComoDia3() {
		reparto.getJornadas().removeIf(j -> j.getNumero() == 3);
		JornadaEntity extraordinaria = new JornadaEntity();
		extraordinaria.setNumero(3);
		extraordinaria.setExtraordinaria(true);
		extraordinaria.setFecha(DIA_1.plusDays(1));
		reparto.addJornada(extraordinaria);

		assertThat(servicio(DIA_3.atTime(10, 0)).liberarReservasSiEmpiezaElDia3()).isEmpty();
	}

	@Test
	void liberarlasAManoUnaSegundaVezSeRechaza() {
		var servicio = servicio(DIA_3.atTime(10, 0));
		servicio.liberarReservas(7L);

		assertThatThrownBy(() -> servicio.liberarReservas(7L))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("Las reservas del reparto de 2027 ya se liberaron");
	}

	@Test
	void trasLiberarlasAManoLaAutomaticaNoHaceNada() {
		var servicio = servicio(DIA_1.atTime(20, 0));
		servicio.liberarReservas(7L);

		assertThat(servicio(DIA_3.atTime(0, 0)).liberarReservasSiEmpiezaElDia3()).isEmpty();
	}

	private RepartoServiceImpl servicio(LocalDateTime ahora) {
		return new RepartoServiceImpl(repartoRepository, mock(JornadaRepository.class), reservaRepository,
				mock(CambioPeriodoDevolucionRepository.class), mock(PlantillaResguardoRepository.class),
				mock(AlquilerRepository.class), mock(RequisitosAlquiler.class), new RepartoMapperImpl(),
				mock(UsuarioActualService.class), Clock.fixed(instante(ahora), MADRID));
	}

	private static Instant instante(LocalDateTime fechaHora) {
		return fechaHora.atZone(MADRID).toInstant();
	}

	private static TunicaEntity tunica(Long id, EstadoTunicaEnum estado) {
		TunicaEntity tunica = new TunicaEntity();
		ReflectionTestUtils.setField(tunica, "id", id);
		tunica.setCodigo(String.valueOf(id));
		tunica.setEstado(estado);
		return tunica;
	}

	private ReservaEntity reserva(TunicaEntity tunica) {
		ReservaEntity reserva = new ReservaEntity();
		reserva.setReparto(reparto);
		reserva.setHermano(new HermanoEntity());
		reserva.setTunica(tunica);
		reserva.setEstado(EstadoReservaEnum.ACTIVA);
		return reserva;
	}

}
