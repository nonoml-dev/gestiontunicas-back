package es.nonomldev.gestiontunicas.seguridad.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
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
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.service.AuthService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioService;

@WebMvcTest({ AuthController.class, UsuarioController.class })
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class CambioPasswordObligatorioTest {

	private static final String CAMBIO_JSON = """
			{"passwordActual":"temporal-1234","passwordNueva":"contrasena-nueva"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private UsuarioService usuarioService;

	@Test
	void conLaContrasenaPendienteNoSePuedeUsarElRestoDeLaApi() throws Exception {
		mvc.perform(get("/api/v1/usuarios").with(administrador(true))).andExpect(status().isForbidden());
		verifyNoInteractions(usuarioService);
	}

	@Test
	void sinCambioPendienteLaApiFunciona() throws Exception {
		when(usuarioService.listar()).thenReturn(List.of());

		mvc.perform(get("/api/v1/usuarios").with(administrador(false))).andExpect(status().isOk());
	}

	@Test
	void conLaContrasenaPendienteSiSePuedeCambiar() throws Exception {
		UsuarioDTO usuario = new UsuarioDTO(1L, "admin.junta", "Nombre", null, CargoUsuarioEnum.ADMINISTRADOR,
				"Administrador", null, true, false, false, null, Set.of(RolCodigoEnum.ADMIN));
		when(authService.cambiarPassword(eq("admin.junta"), any())).thenReturn(new LoginDTO("token", "Bearer",
				Instant.parse("2027-01-01T10:15:00Z"), "refresh", Instant.parse("2027-01-01T22:00:00Z"), usuario));

		mvc.perform(post("/api/v1/auth/cambiar-password").contentType(MediaType.APPLICATION_JSON).content(CAMBIO_JSON)
				.with(administrador(true)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("token"))
				.andExpect(jsonPath("$.usuario.debeCambiarPassword").value(false));

		verify(authService).cambiarPassword("admin.junta",
				new CambioPasswordRequest("temporal-1234", "contrasena-nueva"));
	}

	@Test
	void cambiarLaContrasenaRequiereSesion() throws Exception {
		mvc.perform(post("/api/v1/auth/cambiar-password").contentType(MediaType.APPLICATION_JSON).content(CAMBIO_JSON))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(authService);
	}

	@Test
	void rechazaUnaContrasenaNuevaCorta() throws Exception {
		mvc.perform(post("/api/v1/auth/cambiar-password").contentType(MediaType.APPLICATION_JSON)
				.content("{\"passwordActual\":\"temporal-1234\",\"passwordNueva\":\"corta\"}")
				.with(administrador(true)))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(authService);
	}

	private static RequestPostProcessor administrador(boolean pendiente) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).jwt(token -> {
			token.subject("admin.junta");
			if (pendiente) {
				token.claim(SecurityConfig.CLAIM_DEBE_CAMBIAR_PASSWORD, true);
			}
		});
	}

}
