package es.nonomldev.gestiontunicas.hermano.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import es.nonomldev.gestiontunicas.hermano.dto.CriterioEdadDTO;
import es.nonomldev.gestiontunicas.hermano.mapper.HermanoMapper;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;

class HermanoServiceImplTest {

	private static final PageRequest PAGINA = PageRequest.of(0, 25);

	private final HermanoRepository hermanoRepository = mock(HermanoRepository.class);
	private final RepartoRepository repartoRepository = mock(RepartoRepository.class);
	private HermanoServiceImpl service;

	@BeforeEach
	void preparar() {
		// 18/11/2026: el próximo Jueves Santo es el 25/03/2027
		Clock clock = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneId.of("Europe/Madrid"));
		service = new HermanoServiceImpl(hermanoRepository, repartoRepository, mock(HermanoMapper.class), clock);
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.empty());
		when(hermanoRepository.buscar(any(), any())).thenReturn(Page.empty());
		when(hermanoRepository.buscarEnEdad(any(), any(), any(), any())).thenReturn(Page.empty());
	}

	@Test
	void sinFiltroSalenTodosLosHermanos() {
		service.buscar("Martínez", false, PAGINA);

		verify(hermanoRepository).buscar("martinez", PAGINA);
		verify(hermanoRepository, never()).buscarEnEdad(any(), any(), any(), any());
	}

	@Test
	void sinRepartoAbiertoElFiltroUsaElProximoJuevesSantoY10a15Anios() {
		assertThat(service.criterioEdad()).isEqualTo(new CriterioEdadDTO(LocalDate.of(2027, 3, 25), 10, 15, null));

		service.buscar(null, true, PAGINA);

		// 16 años el mismo Jueves Santo (nacido el 25/03/2011) ya no entra; 10 años ese día (25/03/2017) sí.
		verify(hermanoRepository).buscarEnEdad("", LocalDate.of(2011, 3, 25), LocalDate.of(2017, 3, 25), PAGINA);
	}

	@Test
	void conRepartoAbiertoUsaSuJuevesSantoYSuRangoDeEdad() {
		RepartoEntity reparto = new RepartoEntity();
		reparto.setAnio(2027);
		reparto.setFechaJuevesSanto(LocalDate.of(2027, 3, 25));
		reparto.setEdadMin(11);
		reparto.setEdadMax(14);
		when(repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)).thenReturn(Optional.of(reparto));

		service.buscar("", true, PAGINA);

		assertThat(service.criterioEdad().repartoAnio()).isEqualTo(2027);
		verify(hermanoRepository).buscarEnEdad("", LocalDate.of(2012, 3, 25), LocalDate.of(2016, 3, 25), PAGINA);
	}

}
