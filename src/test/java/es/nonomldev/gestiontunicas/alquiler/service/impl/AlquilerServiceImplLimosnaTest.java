package es.nonomldev.gestiontunicas.alquiler.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.alquiler.dto.EntregaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDTO;
import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.mapper.AlquilerMapper;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.repository.ResguardoRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.alquiler.service.SituacionHermanoService;
import es.nonomldev.gestiontunicas.devolucion.service.ProcesadorDevolucion;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;

/** REP-05: cada entrega guarda la limosna que se le aplicó, la de la jornada extraordinaria o la del reparto. */
class AlquilerServiceImplLimosnaTest {

	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
	private static final LocalDate HOY = LocalDate.of(2027, 3, 15);

	private final AlquilerRepository alquilerRepository = mock(AlquilerRepository.class);
	private final HermanoRepository hermanoRepository = mock(HermanoRepository.class);
	private final TunicaRepository tunicaRepository = mock(TunicaRepository.class);
	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private final JornadaRepository jornadaRepository = mock(JornadaRepository.class);
	private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
	private final SituacionHermanoService situacionService = mock(SituacionHermanoService.class);
	private final UsuarioActualService usuarioActual = mock(UsuarioActualService.class);

	private final AlquilerServiceImpl servicio = new AlquilerServiceImpl(alquilerRepository,
			mock(ResguardoRepository.class), hermanoRepository, tunicaRepository, repartoRepository, jornadaRepository,
			reservaRepository, situacionService, mock(RequisitosAlquiler.class), mock(ProcesadorDevolucion.class),
			mock(AlquilerMapper.class), usuarioActual,
			Clock.fixed(ZonedDateTime.of(HOY.atTime(19, 0), MADRID).toInstant(), MADRID));

	private RepartoEntity reparto;
	private JornadaEntity jornada;
	private TunicaEntity tunica;

	@BeforeEach
	void preparar() {
		reparto = new RepartoEntity();
		ReflectionTestUtils.setField(reparto, "id", 7L);
		reparto.setAnio(2027);
		reparto.setEstado(EstadoRepartoEnum.ABIERTO);
		reparto.setLimosna(new BigDecimal("20.00"));
		reparto.setPlantillaResguardo(new PlantillaResguardoEntity());

		jornada = new JornadaEntity();
		jornada.setNumero(4);
		jornada.setFecha(HOY);
		jornada.setPermiteNuevaAlta(true);
		reparto.addJornada(jornada);

		HermanoEntity hermano = new HermanoEntity();
		ReflectionTestUtils.setField(hermano, "id", 1L);
		hermano.setTelefono1("600000000");

		tunica = new TunicaEntity();
		ReflectionTestUtils.setField(tunica, "id", 42L);
		tunica.setCodigo("42");
		tunica.setEstado(EstadoTunicaEnum.DISPONIBLE);

		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.of(reparto));
		when(hermanoRepository.findById(1L)).thenReturn(Optional.of(hermano));
		when(situacionService.evaluar(1L)).thenReturn(new SituacionHermanoDTO(1L, 7L, 2027, true,
				TipoAlquilerEnum.NUEVA_ALTA, null, null, null, null, null, null, null, false, true));
		when(jornadaRepository.findByRepartoIdAndFecha(7L, HOY)).thenReturn(Optional.of(jornada));
		when(tunicaRepository.findByIdParaActualizar(42L)).thenReturn(Optional.of(tunica));
		when(reservaRepository.findByRepartoIdAndHermanoId(7L, 1L)).thenReturn(Optional.empty());
		when(usuarioActual.obtener()).thenReturn(new UsuarioEntity());
		when(alquilerRepository.save(any())).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	void enUnaJornadaExtraordinariaConLimosnaPropiaGuardaEsa() {
		jornada.setExtraordinaria(true);
		jornada.setLimosna(new BigDecimal("15.50"));

		assertThat(entregar().getLimosna()).isEqualByComparingTo("15.50");
	}

	@Test
	void enUnaJornadaExtraordinariaSinLimosnaGuardaLaDelReparto() {
		jornada.setExtraordinaria(true);
		jornada.setLimosna(null);

		assertThat(entregar().getLimosna()).isEqualByComparingTo("20.00");
	}

	@Test
	void enUnaJornadaOrdinariaGuardaLaDelReparto() {
		jornada.setNumero(2);

		AlquilerEntity alquiler = entregar();

		assertThat(alquiler.getLimosna()).isEqualByComparingTo("20.00");
		assertThat(alquiler.getJornada()).isSameAs(jornada);
	}

	@Test
	void siDespuesCambiaLaLimosnaDelRepartoLaEntregaConservaLaSuya() {
		jornada.setExtraordinaria(true);
		jornada.setLimosna(new BigDecimal("15.50"));
		AlquilerEntity alquiler = entregar();

		reparto.setLimosna(new BigDecimal("25.00"));
		jornada.setLimosna(new BigDecimal("10.00"));

		assertThat(alquiler.getLimosna()).isEqualByComparingTo("15.50");
	}

	private AlquilerEntity entregar() {
		servicio.entregar(new EntregaRequest(1L, 42L, 150, null, false, false, null));
		ArgumentCaptor<AlquilerEntity> guardado = ArgumentCaptor.forClass(AlquilerEntity.class);
		verify(alquilerRepository).save(guardado.capture());
		return guardado.getValue();
	}

}
