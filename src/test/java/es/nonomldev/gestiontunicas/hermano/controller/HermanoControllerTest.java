package es.nonomldev.gestiontunicas.hermano.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.hermano.dto.ContactoHermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;
import es.nonomldev.gestiontunicas.hermano.service.HermanoService;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;

@WebMvcTest(HermanoController.class)
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class HermanoControllerTest {

	private static final String HERMANO_JSON = """
			{"numeroHermano":"1234","nombre":"Juan","apellidos":"García López","fechaNacimiento":"2014-05-10",
			 "telefono1":"600000000","telefono2":null,"email":"juan@example.com"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private HermanoService hermanoService;

	@Test
	void sinTokenDevuelve401() throws Exception {
		mvc.perform(get("/api/v1/hermanos")).andExpect(status().isUnauthorized());
	}

	@Test
	void elListadoDevuelveLosDatosDelExcelYOrdenaPorApellidos() throws Exception {
		when(hermanoService.buscar(eq("garcia"), eq(false), any())).thenReturn(new PageImpl<>(
				List.of(new HermanoResumenDTO(1L, "1234", "García López, Juan", LocalDate.of(2014, 5, 10), true)),
				PageRequest.of(0, 25), 1));

		mvc.perform(get("/api/v1/hermanos").param("q", "garcia").with(rol("CAPILLER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].numeroHermano").value("1234"))
				.andExpect(jsonPath("$.content[0].nombreCompleto").value("García López, Juan"))
				.andExpect(jsonPath("$.page.totalElements").value(1));

		Pageable esperado = PageRequest.of(0, 25, Sort.by("apellidos", "nombre").ascending().and(Sort.by("id")));
		verify(hermanoService).buscar("garcia", false, esperado);
	}

	@Test
	void ordenaPorNumeroDeHermanoComoNumeroYConLosSinNumeroAlFinal() throws Exception {
		when(hermanoService.buscar(any(), anyBoolean(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

		mvc.perform(get("/api/v1/hermanos").param("orden", "numeroHermano").param("direccion", "desc").param("enEdad", "true")
				.with(rol("CAPILLER"))).andExpect(status().isOk());

		Sort esperado = Sort.by(new Sort.Order(Sort.Direction.DESC, "numeroOrden", Sort.NullHandling.NULLS_LAST))
				.and(Sort.by("id"));
		verify(hermanoService).buscar(null, true, PageRequest.of(0, 25, esperado));
	}

	@Test
	void cadaColumnaOrdenaSoloPorLoSuyo() throws Exception {
		when(hermanoService.buscar(any(), anyBoolean(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

		mvc.perform(get("/api/v1/hermanos").param("orden", "fechaNacimiento").with(rol("CAPILLER")))
				.andExpect(status().isOk());

		verify(hermanoService).buscar(null, false,
				PageRequest.of(0, 25, Sort.by("fechaNacimiento").ascending().and(Sort.by("id"))));
	}

	@Test
	void unOrdenDesconocidoDevuelve400EnVezDeOrdenarPorOtraColumna() throws Exception {
		mvc.perform(get("/api/v1/hermanos").param("orden", "edad").with(rol("CAPILLER")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Orden no válido: edad")));
		mvc.perform(get("/api/v1/hermanos").param("direccion", "arriba").with(rol("CAPILLER")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(hermanoService);
	}

	@Test
	void elCapillerNoPuedeCrearNiDarDeBaja() throws Exception {
		mvc.perform(conJson(post("/api/v1/hermanos")).with(rol("CAPILLER"))).andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/hermanos/1").with(rol("CAPILLER"))).andExpect(status().isForbidden());
	}

	@Test
	void administradorPriosteMayordomoYSecretarioPuedenCrear() throws Exception {
		when(hermanoService.crear(any())).thenReturn(new HermanoDTO(1L, "1234", "Juan", "García López",
				"García López, Juan", LocalDate.of(2014, 5, 10), "600000000", null, "juan@example.com", true, true));

		for (String rol : List.of("ADMIN", "PRIOSTE", "MAYORDOMO", "SECRETARIO")) {
			mvc.perform(conJson(post("/api/v1/hermanos")).with(rol(rol)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.telefono1").value("600000000"));
		}
	}

	@Test
	void editarSoloCambiaElContactoAunqueLleguenLosDatosDelHermano() throws Exception {
		when(hermanoService.actualizarContacto(eq(1L), any())).thenReturn(new HermanoDTO(1L, "1234", "Juan",
				"García López", "García López, Juan", LocalDate.of(2014, 5, 10), "600000000", null, "juan@example.com",
				true, true));

		mvc.perform(put("/api/v1/hermanos/1").contentType(MediaType.APPLICATION_JSON).content(HERMANO_JSON)
				.with(rol("SECRETARIO")))
				.andExpect(status().isOk());

		verify(hermanoService).actualizarContacto(1L,
				new ContactoHermanoRequest("600000000", null, "juan@example.com"));
		mvc.perform(put("/api/v1/hermanos/1").contentType(MediaType.APPLICATION_JSON).content(HERMANO_JSON)
				.with(rol("CAPILLER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void datosNoValidosDevuelven400() throws Exception {
		mvc.perform(post("/api/v1/hermanos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"\",\"apellidos\":\"\",\"email\":\"no-es-un-email\"}")
				.with(rol("SECRETARIO")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void hermanoInexistenteDevuelve404ConElMotivo() throws Exception {
		when(hermanoService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("el hermano", 99L));

		mvc.perform(get("/api/v1/hermanos/99").with(rol("CAPILLER")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("No existe el hermano con id 99"));
	}

	private static MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder peticion) {
		return peticion.contentType(MediaType.APPLICATION_JSON).content(HERMANO_JSON);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

}
