package es.nonomldev.gestiontunicas.tunica.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;
import es.nonomldev.gestiontunicas.tunica.dto.PiezaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDTO;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.service.TunicaService;

@WebMvcTest(TunicaController.class)
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class TunicaControllerTest {

	private static final String TUNICA_JSON = "{\"codigo\":\"42\",\"medidaCm\":158,\"dobladilloCm\":6}";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private TunicaService tunicaService;

	@Test
	void elInventarioFiltraPorEstadosYMedidaYOrdenaPorCodigoComoNumero() throws Exception {
		when(tunicaService.buscar(any(), any(), any(), any())).thenReturn(new PageImpl<>(
				List.of(new TunicaResumenDTO(1L, "42", 158, 6, 164, EstadoTunicaEnum.DISPONIBLE)),
				PageRequest.of(0, 25), 1));

		mvc.perform(get("/api/v1/tunicas").param("estado", "DISPONIBLE", "EN_REPARACION")
				.param("medidaMin", "150").param("medidaMax", "170").with(rol("CAPILLER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].codigo").value("42"))
				.andExpect(jsonPath("$.content[0].medidaTotalCm").value(164))
				.andExpect(jsonPath("$.page.totalElements").value(1));

		verify(tunicaService).buscar(EnumSet.of(EstadoTunicaEnum.DISPONIBLE, EstadoTunicaEnum.EN_REPARACION), 150, 170,
				PageRequest.of(0, 25, ordenPorCodigo().ascending().and(Sort.by("id"))));
	}

	@Test
	void sinFiltrosElServicioRecibeNulos() throws Exception {
		when(tunicaService.buscar(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

		mvc.perform(get("/api/v1/tunicas").param("orden", "medidaTotal").param("direccion", "desc")
				.with(rol("MAYORDOMO"))).andExpect(status().isOk());

		verify(tunicaService).buscar(null, null, null,
				PageRequest.of(0, 25, Sort.by("medidaTotalCm").descending().and(Sort.by("id"))));
	}

	@Test
	void parametrosDelListadoNoValidosDevuelven400() throws Exception {
		mvc.perform(get("/api/v1/tunicas").param("orden", "talla").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(startsWith("Orden no válido: talla")));
		mvc.perform(get("/api/v1/tunicas").param("direccion", "arriba").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/tunicas").param("medidaMin", "170").param("medidaMax", "150").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("La medida mínima no puede ser mayor que la máxima"));
		mvc.perform(get("/api/v1/tunicas").param("estado", "PERDIDA").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(tunicaService);
	}

	@Test
	void laPropuestaUsaLaToleranciaDelRepartoSiNoSeIndica() throws Exception {
		when(tunicaService.proponer(anyInt(), any())).thenReturn(List.of(
				new TunicaResumenDTO(1L, "42", 158, 6, 164, EstadoTunicaEnum.DISPONIBLE)));

		mvc.perform(get("/api/v1/tunicas/propuesta").param("alturaCm", "163").with(rol("CAPILLER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].codigo").value("42"));
		verify(tunicaService).proponer(163, null);

		mvc.perform(get("/api/v1/tunicas/propuesta").param("alturaCm", "163").param("toleranciaCm", "8")
				.with(rol("PRIOSTE"))).andExpect(status().isOk());
		verify(tunicaService).proponer(163, 8);
	}

	@Test
	void laPropuestaRechazaAlturasYToleranciasFueraDeRango() throws Exception {
		mvc.perform(get("/api/v1/tunicas/propuesta").param("alturaCm", "30").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("La altura debe estar entre 60 y 220 cm"));
		mvc.perform(get("/api/v1/tunicas/propuesta").param("alturaCm", "160").param("toleranciaCm", "-1")
				.with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("La tolerancia debe estar entre 0 y 50 cm"));
		mvc.perform(get("/api/v1/tunicas/propuesta").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(tunicaService);
	}

	@Test
	void avisaSiElCodigoFueDeUnHermano() throws Exception {
		when(tunicaService.codigoFueDeHermano("42")).thenReturn(true);

		mvc.perform(get("/api/v1/tunicas/aviso-codigo").param("codigo", "42").with(rol("PRIOSTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("42"))
				.andExpect(jsonPath("$.fueDeHermano").value(true));
	}

	@Test
	void alCrearDevuelveLaTunicaConSusCincoPiezas() throws Exception {
		when(tunicaService.crear(any())).thenReturn(tunica(EstadoTunicaEnum.DISPONIBLE));

		mvc.perform(post("/api/v1/tunicas").contentType(MediaType.APPLICATION_JSON).content(TUNICA_JSON)
				.with(rol("PRIOSTE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.medidaTotalCm").value(164))
				.andExpect(jsonPath("$.piezas.length()").value(5));

		verify(tunicaService).crear(new TunicaRequest("42", 158, 6, null, null));
	}

	@Test
	void codigosNoValidosDevuelven400() throws Exception {
		for (String codigo : List.of("0", "1000", "042", "M-12", "m-012", "A-001")) {
			mvc.perform(post("/api/v1/tunicas").contentType(MediaType.APPLICATION_JSON)
					.content("{\"codigo\":\"" + codigo + "\",\"medidaCm\":158,\"dobladilloCm\":6}")
					.with(rol("PRIOSTE")))
					.andExpect(status().isBadRequest());
		}
		verifyNoInteractions(tunicaService);
	}

	@Test
	void unCodigoEnUsoDevuelve409ConElMotivo() throws Exception {
		when(tunicaService.crear(any()))
				.thenThrow(new ReglaNegocioException("Ya hay una túnica en el inventario con el código 42"));

		mvc.perform(post("/api/v1/tunicas").contentType(MediaType.APPLICATION_JSON).content(TUNICA_JSON)
				.with(rol("ADMIN")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Ya hay una túnica en el inventario con el código 42"));
	}

	@Test
	void laBajaPideMotivo() throws Exception {
		when(tunicaService.darDeBaja(eq(1L), any())).thenReturn(tunica(EstadoTunicaEnum.BAJA));

		mvc.perform(post("/api/v1/tunicas/1/baja").contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"\"}").with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/tunicas/1/baja").contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"Rota sin arreglo\"}").with(rol("PRIOSTE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("BAJA"));

		verify(tunicaService).darDeBaja(1L, new MotivoRequest("Rota sin arreglo"));
	}

	@Test
	void tunicaInexistenteDevuelve404() throws Exception {
		when(tunicaService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("la túnica", 99L));

		mvc.perform(get("/api/v1/tunicas/99").with(rol("SECRETARIO")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("No existe la túnica con id 99"));
	}

	private static Sort ordenPorCodigo() {
		return org.springframework.data.jpa.domain.JpaSort.unsafe("length(t.codigo)").and(Sort.by("codigo"));
	}

	private static TunicaDTO tunica(EstadoTunicaEnum estado) {
		List<PiezaDTO> piezas = List.of(TipoPiezaEnum.values()).stream()
				.map(tipo -> new PiezaDTO(null, tipo, EstadoPiezaEnum.BUEN_ESTADO, null))
				.toList();
		return new TunicaDTO(1L, "42", false, 158, 6, 164, estado, LocalDate.of(2026, 11, 24), null, null, null, null,
				piezas);
	}

	private static RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

}
