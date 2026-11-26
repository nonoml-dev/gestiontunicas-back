package es.nonomldev.gestiontunicas.reparto.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.service.RepartoService;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;

/** REP-08: una transición no válida llega al cliente como 409 con su motivo; unos datos incoherentes, como 400. */
@WebMvcTest(RepartoController.class)
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class RepartoControllerTest {

	private static final String JORNADA = """
			{"fecha":"2027-03-08","horaInicio":"18:00","horaFin":"21:00","limosna":null,
			 "permiteRenovacion":true,"permiteNuevaAlta":true,"permiteCambio":true}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RepartoService repartoService;

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"abrir            | El reparto de 2027 ya está abierto o cerrado",
			"cerrar           | El reparto de 2027 no está abierto",
			"liberar-reservas | Las reservas del reparto de 2027 ya se liberaron" })
	void unaTransicionNoValidaDevuelve409ConElMotivo(String accion, String motivo) throws Exception {
		ReglaNegocioException error = new ReglaNegocioException(motivo);
		switch (accion) {
			case "abrir" -> when(repartoService.abrir(7L)).thenThrow(error);
			case "cerrar" -> when(repartoService.cerrar(7L)).thenThrow(error);
			default -> when(repartoService.liberarReservas(7L)).thenThrow(error);
		}

		mvc.perform(post("/api/v1/repartos/7/" + accion).with(rol("PRIOSTE")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.title").value("Operación no permitida"))
				.andExpect(jsonPath("$.detail").value(motivo));
	}

	@Test
	void unRepartoQueNoExisteDevuelve404() throws Exception {
		when(repartoService.abrir(99L)).thenThrow(new RecursoNoEncontradoException("el reparto", 99L));

		mvc.perform(post("/api/v1/repartos/99/abrir").with(rol("PRIOSTE")))
				.andExpect(status().isNotFound());
	}

	@Test
	void sinRepartoAbiertoDevuelve204() throws Exception {
		when(repartoService.obtenerAbierto()).thenReturn(Optional.empty());

		mvc.perform(get("/api/v1/repartos/abierto").with(rol("SECRETARIO")))
				.andExpect(status().isNoContent());
	}

	@Test
	void unRepartoConEdadesOFechasIncoherentesSeRechazaSinLlegarAlServicio() throws Exception {
		String reparto = """
				{"anio":2027,"edadMin":15,"edadMax":10,
				 "periodoDevolucion":{"inicio":"2027-04-30","fin":"2027-04-05","plazoGraciaMeses":2},
				 "limosna":20,
				 "tarifas":{"importePiezaPrincipal":30,"importeTalega":5,"importeRetraso":10,"importeCingulo":5,
				            "importeSuciedad":10,"importeCargoTotal":150},
				 "toleranciaMedidaCm":3,"modoTransicion":true,"jornadas":[%s,%s,%s]}
				""".formatted(JORNADA, JORNADA, JORNADA);

		mvc.perform(post("/api/v1/repartos").contentType(MediaType.APPLICATION_JSON).content(reparto).with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repartoService);
	}

	@Test
	void unRepartoConDosJornadasSeRechaza() throws Exception {
		String reparto = """
				{"anio":2027,"edadMin":10,"edadMax":15,
				 "periodoDevolucion":{"inicio":"2027-04-05","fin":"2027-04-30","plazoGraciaMeses":2},
				 "limosna":20,
				 "tarifas":{"importePiezaPrincipal":30,"importeTalega":5,"importeRetraso":10,"importeCingulo":5,
				            "importeSuciedad":10,"importeCargoTotal":150},
				 "toleranciaMedidaCm":3,"modoTransicion":true,"jornadas":[%s,%s]}
				""".formatted(JORNADA, JORNADA);

		mvc.perform(post("/api/v1/repartos").contentType(MediaType.APPLICATION_JSON).content(reparto).with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repartoService);
	}

	@Test
	void unaJornadaExtraordinariaConElHorarioAlReves() throws Exception {
		String jornada = JORNADA.replace("\"18:00\"", "\"22:00\"");

		mvc.perform(post("/api/v1/repartos/7/jornadas-extraordinarias").contentType(MediaType.APPLICATION_JSON)
				.content(jornada).with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repartoService);
	}

	@Test
	void cambiarElPeriodoSinMotivoSeRechaza() throws Exception {
		String cambio = """
				{"periodo":{"inicio":"2027-04-05","fin":"2027-05-15","plazoGraciaMeses":2},"motivo":"  "}
				""";

		mvc.perform(patch("/api/v1/repartos/7/periodo-devolucion").contentType(MediaType.APPLICATION_JSON)
				.content(cambio).with(rol("PRIOSTE")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(repartoService);
	}

	private static RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

}
