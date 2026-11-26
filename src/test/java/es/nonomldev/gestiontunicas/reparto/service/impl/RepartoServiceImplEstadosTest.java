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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.PeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.TarifaSancionesDTO;
import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapperImpl;
import es.nonomldev.gestiontunicas.reparto.repository.CambioPeriodoDevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;

/**
 * REP-08: estados del reparto (BORRADOR → ABIERTO → CERRADO), el Jueves Santo, las reservas al abrirlo y el
 * cambio del periodo de devolución. Cada transición no válida se rechaza sin tocar nada.
 */
class RepartoServiceImplEstadosTest {

	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
	private static final Instant AHORA = Instant.parse("2027-03-01T18:00:00Z");

	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
	private final CambioPeriodoDevolucionRepository cambioRepository = mock(CambioPeriodoDevolucionRepository.class);
	private final PlantillaResguardoRepository plantillaRepository = mock(PlantillaResguardoRepository.class);
	private final AlquilerRepository alquilerRepository = mock(AlquilerRepository.class);
	private final RequisitosAlquiler requisitos = mock(RequisitosAlquiler.class);
	private final UsuarioActualService usuarioActual = mock(UsuarioActualService.class);

	private final RepartoServiceImpl servicio = new RepartoServiceImpl(repartoRepository, mock(JornadaRepository.class),
			reservaRepository, cambioRepository, plantillaRepository, alquilerRepository, requisitos,
			new RepartoMapperImpl(), usuarioActual, Clock.fixed(AHORA, MADRID));

	@BeforeEach
	void preparar() {
		when(repartoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		when(reservaRepository.findByRepartoIdAndEstado(anyLong(), any())).thenReturn(List.of());
	}

	// ---- Jueves Santo ----

	@Nested
	class JuevesSanto {

		@ParameterizedTest(name = "{0}: {1}")
		@CsvSource({
				"2026, 2026-04-02",
				"2027, 2027-03-25",
				"2028, 2028-04-13",
				"2035, 2035-03-22",
				// Pascua más tardía posible (25 de abril)
				"2038, 2038-04-22",
				"2100, 2100-03-25",
				// Pascua más temprana posible (22 de marzo)
				"2285, 2285-03-19" })
		void seCalculaComoDomingoDeResurreccionMenosTresDias(int anio, LocalDate juevesSanto) {
			assertThat(RepartoEntity.calcularJuevesSanto(anio)).isEqualTo(juevesSanto);
		}

		@Test
		void sinFechaSeCalculaYNoQuedaComoManual() {
			RepartoDTO dto = servicio.crear(peticion(2027, null));

			assertThat(dto.fechaJuevesSanto()).isEqualTo(LocalDate.of(2027, 3, 25));
			assertThat(dto.juevesSantoManual()).isFalse();
		}

		@Test
		void conFechaSeRespetaYQuedaComoManual() {
			RepartoDTO dto = servicio.crear(peticion(2027, LocalDate.of(2027, 3, 26)));

			assertThat(dto.fechaJuevesSanto()).isEqualTo(LocalDate.of(2027, 3, 26));
			assertThat(dto.juevesSantoManual()).isTrue();
		}

		@Test
		void alCambiarElAnioEnBorradorSeRecalcula() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);

			RepartoDTO dto = servicio.actualizar(7L, peticion(2028, null));

			assertThat(dto.fechaJuevesSanto()).isEqualTo(LocalDate.of(2028, 4, 13));
			assertThat(reparto.getAnio()).isEqualTo(2028);
		}

		@Test
		void alQuitarLaFechaManualVuelveALaCalculada() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);
			reparto.setFechaJuevesSanto(LocalDate.of(2027, 3, 26));
			reparto.setJuevesSantoManual(true);

			RepartoDTO dto = servicio.actualizar(7L, peticion(2027, null));

			assertThat(dto.fechaJuevesSanto()).isEqualTo(LocalDate.of(2027, 3, 25));
			assertThat(dto.juevesSantoManual()).isFalse();
		}

	}

	// ---- Crear y editar ----

	@Nested
	class CrearYEditar {

		@Test
		void naceEnBorradorConSusTresJornadasOrdinariasQueUsanLaLimosnaDelReparto() {
			RepartoDTO dto = servicio.crear(peticion(2027, null));

			assertThat(dto.estado()).isEqualTo(EstadoRepartoEnum.BORRADOR);
			assertThat(dto.jornadas()).extracting("numero").containsExactly(1, 2, 3);
			assertThat(dto.jornadas()).allSatisfy(j -> {
				assertThat(j.extraordinaria()).isFalse();
				// La petición trae una limosna en cada jornada, pero las ordinarias usan la del reparto
				assertThat(j.limosna()).isNull();
			});
			assertThat(dto.fechaApertura()).isNull();
		}

		@Test
		void noAdmiteDosRepartosDelMismoAnio() {
			when(repartoRepository.existsByAnio(2027)).thenReturn(true);

			assertThatThrownBy(() -> servicio.crear(peticion(2027, null)))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("Ya existe el reparto de 2027");
			verify(repartoRepository, never()).save(any());
		}

		@Test
		void editarActualizaLasJornadasEnSuSitioSinDuplicarlas() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);

			servicio.actualizar(7L, peticion(2027, null));

			assertThat(reparto.getJornadas()).hasSize(3);
			assertThat(reparto.getJornadas()).extracting(JornadaEntity::getFecha)
					.containsExactly(LocalDate.of(2027, 3, 8), LocalDate.of(2027, 3, 9), LocalDate.of(2027, 3, 10));
		}

		@Test
		void editarNoPuedeLlevarloAUnAnioQueYaTieneReparto() {
			existente(EstadoRepartoEnum.BORRADOR);
			when(repartoRepository.existsByAnio(2028)).thenReturn(true);

			assertThatThrownBy(() -> servicio.actualizar(7L, peticion(2028, null)))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("Ya existe el reparto de 2028");
		}

		@ParameterizedTest
		@EnumSource(value = EstadoRepartoEnum.class, names = { "ABIERTO", "CERRADO" })
		void soloSeEditaEnBorrador(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);
			BigDecimal limosna = reparto.getLimosna();

			assertThatThrownBy(() -> servicio.actualizar(7L, peticion(2027, null)))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2027 solo se puede modificar en borrador");
			assertThat(reparto.getLimosna()).isEqualTo(limosna);
		}

		@Test
		void unRepartoQueNoExisteDa404() {
			when(repartoRepository.findWithJornadasById(99L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
			assertThatThrownBy(() -> servicio.abrir(99L)).isInstanceOf(RecursoNoEncontradoException.class);
		}

	}

	// ---- Abrir ----

	@Nested
	class Abrir {

		@Test
		void deBorradorPasaAAbiertoConSuFecha() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);

			RepartoDTO dto = servicio.abrir(7L);

			assertThat(dto.estado()).isEqualTo(EstadoRepartoEnum.ABIERTO);
			assertThat(reparto.getFechaApertura()).isEqualTo(AHORA);
		}

		@ParameterizedTest
		@EnumSource(value = EstadoRepartoEnum.class, names = { "ABIERTO", "CERRADO" })
		void soloSeAbreUnBorrador(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);

			assertThatThrownBy(() -> servicio.abrir(7L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2027 ya está abierto o cerrado");
			assertThat(reparto.getEstado()).isEqualTo(estado);
		}

		@Test
		void noPuedeHaberDosRepartosAbiertos() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);
			RepartoEntity otro = new RepartoEntity();
			otro.setAnio(2026);
			when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.of(otro));

			assertThatThrownBy(() -> servicio.abrir(7L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2026 sigue abierto: ciérralo antes");
			assertThat(reparto.getEstado()).isEqualTo(EstadoRepartoEnum.BORRADOR);
		}

		@Test
		void sinPlantillaDelResguardoNoSeAbre() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);
			reparto.setPlantillaResguardo(null);

			assertThatThrownBy(() -> servicio.abrir(7L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("Sube la plantilla del resguardo antes de abrir el reparto");
			assertThat(reparto.getEstado()).isEqualTo(EstadoRepartoEnum.BORRADOR);
		}

		@Test
		void enModoTransicionNoReservaNada() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.BORRADOR);
			reparto.setModoTransicion(true);

			servicio.abrir(7L);

			verify(repartoRepository, never()).findByAnio(any());
			verify(reservaRepository, never()).save(any());
		}

		@Test
		void sinRepartoElAnioAnteriorNoReservaNada() {
			existente(EstadoRepartoEnum.BORRADOR);
			when(repartoRepository.findByAnio(2026)).thenReturn(Optional.empty());

			servicio.abrir(7L);

			verify(reservaRepository, never()).save(any());
		}

	}

	// ---- Reservas al abrir ----

	@Nested
	class ReservasAlAbrir {

		private RepartoEntity reparto;
		private final List<AlquilerEntity> devueltos = new ArrayList<>();

		@BeforeEach
		void anioAnterior() {
			reparto = existente(EstadoRepartoEnum.BORRADOR);
			RepartoEntity anterior = new RepartoEntity();
			ReflectionTestUtils.setField(anterior, "id", 6L);
			anterior.setAnio(2026);
			when(repartoRepository.findByAnio(2026)).thenReturn(Optional.of(anterior));
			when(alquilerRepository.findByRepartoIdAndEstado(6L, EstadoAlquilerEnum.DEVUELTO)).thenReturn(devueltos);
			when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
		}

		@Test
		void reservaSuTunicaAQuienRenueva() {
			AlquilerEntity alquiler = devuelto(1L, 100L, 42L, EstadoTunicaEnum.DISPONIBLE);
			renueva(alquiler);

			servicio.abrir(7L);

			ReservaEntity reserva = reservaGuardada();
			assertThat(reserva.getReparto()).isSameAs(reparto);
			assertThat(reserva.getHermano()).isSameAs(alquiler.getHermano());
			assertThat(reserva.getTunica()).isSameAs(alquiler.getTunica());
			assertThat(reserva.getAlquilerAnterior()).isSameAs(alquiler);
			assertThat(reserva.getEstado()).isEqualTo(EstadoReservaEnum.ACTIVA);
			assertThat(alquiler.getTunica().getEstado()).isEqualTo(EstadoTunicaEnum.RESERVADA);
		}

		@ParameterizedTest
		@EnumSource(value = EstadoTunicaEnum.class, names = "DISPONIBLE", mode = EnumSource.Mode.EXCLUDE)
		void siSuTunicaNoEstaDisponibleNoSeReserva(EstadoTunicaEnum estado) {
			AlquilerEntity alquiler = devuelto(1L, 100L, 42L, estado);
			renueva(alquiler);

			servicio.abrir(7L);

			verify(reservaRepository, never()).save(any());
			assertThat(alquiler.getTunica().getEstado()).isEqualTo(estado);
		}

		@Test
		void siNoPuedeAlquilarNoSeLeReserva() {
			AlquilerEntity alquiler = devuelto(1L, 100L, 42L, EstadoTunicaEnum.DISPONIBLE);
			renueva(alquiler);
			when(requisitos.motivoNoPuedeAlquilar(alquiler.getHermano(), reparto))
					.thenReturn(Optional.of("Sanción pendiente de 15,00 €"));

			servicio.abrir(7L);

			verify(reservaRepository, never()).save(any());
			assertThat(alquiler.getTunica().getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		}

		@Test
		void siNoLeTocaRenovarNoSeLeReserva() {
			AlquilerEntity alquiler = devuelto(1L, 100L, 42L, EstadoTunicaEnum.DISPONIBLE);
			when(requisitos.alquilerQueDaRenovacion(100L, reparto)).thenReturn(Optional.empty());

			servicio.abrir(7L);

			verify(reservaRepository, never()).save(any());
		}

		@Test
		void conVariosAlquileresDelMismoHermanoSoloSeReservaElQueDaLaRenovacion() {
			// Cambió de túnica el año pasado: devolvió la 42 en el cambio y la 43 al final
			AlquilerEntity cambiada = devuelto(1L, 100L, 42L, EstadoTunicaEnum.DISPONIBLE);
			AlquilerEntity ultima = devuelto(2L, 100L, 43L, EstadoTunicaEnum.DISPONIBLE);
			when(requisitos.alquilerQueDaRenovacion(100L, reparto)).thenReturn(Optional.of(ultima));
			when(requisitos.motivoNoPuedeAlquilar(any(), any())).thenReturn(Optional.empty());

			servicio.abrir(7L);

			assertThat(reservaGuardada().getTunica()).isSameAs(ultima.getTunica());
			assertThat(cambiada.getTunica().getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		}

		@Test
		void cadaHermanoQueRenuevaTieneSuReserva() {
			AlquilerEntity uno = devuelto(1L, 100L, 42L, EstadoTunicaEnum.DISPONIBLE);
			AlquilerEntity otro = devuelto(2L, 200L, 43L, EstadoTunicaEnum.DISPONIBLE);
			renueva(uno);
			renueva(otro);

			servicio.abrir(7L);

			ArgumentCaptor<ReservaEntity> reservas = ArgumentCaptor.forClass(ReservaEntity.class);
			verify(reservaRepository, org.mockito.Mockito.times(2)).save(reservas.capture());
			assertThat(reservas.getAllValues()).extracting(r -> r.getTunica().getCodigo()).containsExactly("42", "43");
		}

		private AlquilerEntity devuelto(Long id, Long hermanoId, Long tunicaId, EstadoTunicaEnum estadoTunica) {
			HermanoEntity hermano = new HermanoEntity();
			ReflectionTestUtils.setField(hermano, "id", hermanoId);
			TunicaEntity tunica = new TunicaEntity();
			ReflectionTestUtils.setField(tunica, "id", tunicaId);
			tunica.setCodigo(String.valueOf(tunicaId));
			tunica.setEstado(estadoTunica);
			AlquilerEntity alquiler = new AlquilerEntity();
			ReflectionTestUtils.setField(alquiler, "id", id);
			alquiler.setHermano(hermano);
			alquiler.setTunica(tunica);
			alquiler.setEstado(EstadoAlquilerEnum.DEVUELTO);
			devueltos.add(alquiler);
			return alquiler;
		}

		private void renueva(AlquilerEntity alquiler) {
			when(requisitos.alquilerQueDaRenovacion(alquiler.getHermano().getId(), reparto)).thenReturn(Optional.of(alquiler));
			when(requisitos.motivoNoPuedeAlquilar(alquiler.getHermano(), reparto)).thenReturn(Optional.empty());
		}

		private ReservaEntity reservaGuardada() {
			ArgumentCaptor<ReservaEntity> reserva = ArgumentCaptor.forClass(ReservaEntity.class);
			verify(reservaRepository).save(reserva.capture());
			return reserva.getValue();
		}

	}

	// ---- Cerrar, liberar y plantilla ----

	@Nested
	class CerrarYOtras {

		@Test
		void deAbiertoPasaACerradoYLiberaLasReservasQueQueden() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.ABIERTO);
			TunicaEntity tunica = new TunicaEntity();
			tunica.setEstado(EstadoTunicaEnum.RESERVADA);
			ReservaEntity reserva = new ReservaEntity();
			reserva.setTunica(tunica);
			reserva.setEstado(EstadoReservaEnum.ACTIVA);
			when(reservaRepository.findByRepartoIdAndEstado(7L, EstadoReservaEnum.ACTIVA)).thenReturn(List.of(reserva));

			RepartoDTO dto = servicio.cerrar(7L);

			assertThat(dto.estado()).isEqualTo(EstadoRepartoEnum.CERRADO);
			assertThat(reparto.getFechaCierre()).isEqualTo(AHORA);
			assertThat(reserva.getEstado()).isEqualTo(EstadoReservaEnum.LIBERADA);
			assertThat(tunica.getEstado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		}

		@ParameterizedTest
		@EnumSource(value = EstadoRepartoEnum.class, names = { "BORRADOR", "CERRADO" })
		void soloSeCierraUnRepartoAbierto(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);

			assertThatThrownBy(() -> servicio.cerrar(7L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2027 no está abierto");
			assertThat(reparto.getEstado()).isEqualTo(estado);
			assertThat(reparto.getFechaCierre()).isNull();
		}

		@ParameterizedTest
		@EnumSource(value = EstadoRepartoEnum.class, names = { "BORRADOR", "CERRADO" })
		void soloSeLiberanLasReservasDeUnRepartoAbierto(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);

			assertThatThrownBy(() -> servicio.liberarReservas(7L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2027 no está abierto");
			assertThat(reparto.getReservasLiberadasEn()).isNull();
		}

		@ParameterizedTest
		@EnumSource(value = EstadoRepartoEnum.class, names = { "BORRADOR", "ABIERTO" })
		void laPlantillaSeCambiaMientrasNoEsteCerrado(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);
			PlantillaResguardoEntity nueva = new PlantillaResguardoEntity();
			nueva.setNombre("Recibo 2027");
			when(plantillaRepository.findById(3L)).thenReturn(Optional.of(nueva));

			assertThat(servicio.asignarPlantilla(7L, 3L).plantillaResguardoNombre()).isEqualTo("Recibo 2027");
			assertThat(reparto.getPlantillaResguardo()).isSameAs(nueva);
		}

		@Test
		void unRepartoCerradoNoCambiaDePlantilla() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.CERRADO);
			PlantillaResguardoEntity actual = reparto.getPlantillaResguardo();

			assertThatThrownBy(() -> servicio.asignarPlantilla(7L, 3L))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessage("El reparto de 2027 está cerrado");
			assertThat(reparto.getPlantillaResguardo()).isSameAs(actual);
		}

		@Test
		void unaPlantillaQueNoExisteDa404() {
			existente(EstadoRepartoEnum.BORRADOR);
			when(plantillaRepository.findById(3L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> servicio.asignarPlantilla(7L, 3L)).isInstanceOf(RecursoNoEncontradoException.class);
		}

	}

	// ---- Periodo de devolución ----

	@Nested
	class PeriodoDevolucion {

		@ParameterizedTest
		@EnumSource(EstadoRepartoEnum.class)
		void seCambiaConMotivoYQuedaEnElHistorial(EstadoRepartoEnum estado) {
			RepartoEntity reparto = existente(estado);
			UsuarioEntity prioste = new UsuarioEntity();
			prioste.setUsername("prioste");
			when(usuarioActual.obtener()).thenReturn(prioste);

			RepartoDTO dto = servicio.cambiarPeriodoDevolucion(7L, new CambioPeriodoDevolucionRequest(
					new PeriodoDevolucionRequest(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 5, 15), 3), "Lluvias"));

			assertThat(dto.periodoDevolucion().fin()).isEqualTo(LocalDate.of(2027, 5, 15));
			assertThat(dto.periodoDevolucion().finPlazoGracia()).isEqualTo(LocalDate.of(2027, 8, 15));
			ArgumentCaptor<CambioPeriodoDevolucionEntity> cambio = ArgumentCaptor.forClass(CambioPeriodoDevolucionEntity.class);
			verify(cambioRepository).save(cambio.capture());
			assertThat(cambio.getValue().getReparto()).isSameAs(reparto);
			assertThat(cambio.getValue().getAnterior().getFin()).isEqualTo(LocalDate.of(2027, 4, 30));
			assertThat(cambio.getValue().getAnterior().getPlazoGraciaMeses()).isEqualTo(2);
			assertThat(cambio.getValue().getNuevo().getFin()).isEqualTo(LocalDate.of(2027, 5, 15));
			assertThat(cambio.getValue().getMotivo()).isEqualTo("Lluvias");
			assertThat(cambio.getValue().getUsuario()).isSameAs(prioste);
			assertThat(cambio.getValue().getFecha()).isEqualTo(AHORA);
		}

		@Test
		void elHistorialGuardaUnaCopiaQueNoCambiaConElReparto() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.ABIERTO);
			when(usuarioActual.obtener()).thenReturn(new UsuarioEntity());

			servicio.cambiarPeriodoDevolucion(7L, new CambioPeriodoDevolucionRequest(
					new PeriodoDevolucionRequest(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 5, 15), 2), "Lluvias"));
			ArgumentCaptor<CambioPeriodoDevolucionEntity> cambio = ArgumentCaptor.forClass(CambioPeriodoDevolucionEntity.class);
			verify(cambioRepository).save(cambio.capture());

			reparto.getPeriodoDevolucion().setFin(LocalDate.of(2027, 6, 1));
			assertThat(cambio.getValue().getNuevo().getFin()).isEqualTo(LocalDate.of(2027, 5, 15));
		}

		@Test
		void trasCerrarElPlazoDeGraciaYaNoSeCambia() {
			RepartoEntity reparto = existente(EstadoRepartoEnum.CERRADO);
			reparto.setFinGraciaProcesadoEn(AHORA);

			assertThatThrownBy(() -> servicio.cambiarPeriodoDevolucion(7L, new CambioPeriodoDevolucionRequest(
					new PeriodoDevolucionRequest(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 5, 15), 2), "Tarde")))
					.isInstanceOf(ReglaNegocioException.class)
					.hasMessageContaining("plazo de gracia ya se ha cerrado");
			verify(cambioRepository, never()).save(any());
			assertThat(reparto.getPeriodoDevolucion().getFin()).isEqualTo(LocalDate.of(2027, 4, 30));
		}

	}

	// ---- Ayudas ----

	/** Reparto 2027 con id 7, en el estado indicado, con plantilla y sus 3 jornadas. */
	private RepartoEntity existente(EstadoRepartoEnum estado) {
		RepartoEntity reparto = new RepartoEntity();
		ReflectionTestUtils.setField(reparto, "id", 7L);
		reparto.setAnio(2027);
		reparto.setFechaJuevesSanto(LocalDate.of(2027, 3, 25));
		reparto.setEdadMin(10);
		reparto.setEdadMax(15);
		reparto.setEstado(estado);
		reparto.setLimosna(new BigDecimal("20.00"));
		reparto.setToleranciaMedidaCm(3);
		reparto.setPeriodoDevolucion(new PeriodoDevolucionEntity(LocalDate.of(2027, 4, 5), LocalDate.of(2027, 4, 30), 2));
		PlantillaResguardoEntity plantilla = new PlantillaResguardoEntity();
		plantilla.setNombre("Recibo");
		reparto.setPlantillaResguardo(plantilla);
		for (int i = 0; i < 3; i++) {
			JornadaEntity jornada = new JornadaEntity();
			jornada.setNumero(i + 1);
			jornada.setFecha(LocalDate.of(2027, 3, 1 + i));
			reparto.addJornada(jornada);
		}
		when(repartoRepository.findWithJornadasById(7L)).thenReturn(Optional.of(reparto));
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO))
				.thenReturn(estado == EstadoRepartoEnum.ABIERTO ? Optional.of(reparto) : Optional.empty());
		return reparto;
	}

	private static RepartoRequest peticion(int anio, LocalDate juevesSanto) {
		List<JornadaRequest> jornadas = List.of(
				new JornadaRequest(LocalDate.of(anio, 3, 8), null, null, new BigDecimal("5"), true, false, false),
				new JornadaRequest(LocalDate.of(anio, 3, 9), null, null, new BigDecimal("5"), true, true, false),
				new JornadaRequest(LocalDate.of(anio, 3, 10), null, null, new BigDecimal("5"), true, true, true));
		TarifaSancionesDTO tarifas = new TarifaSancionesDTO(new BigDecimal("30"), new BigDecimal("5"),
				new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("150"));
		return new RepartoRequest(anio, juevesSanto, 10, 15,
				new PeriodoDevolucionRequest(LocalDate.of(anio, 4, 5), LocalDate.of(anio, 4, 30), 2), "Casa de hermandad",
				new BigDecimal("20.00"), tarifas, 3, false, jornadas);
	}

}
