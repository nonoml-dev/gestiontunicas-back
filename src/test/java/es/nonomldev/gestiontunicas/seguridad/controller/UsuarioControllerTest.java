package es.nonomldev.gestiontunicas.seguridad.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.service.RolService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioService;

@WebMvcTest({ UsuarioController.class, RolController.class })
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class UsuarioControllerTest {

	private static final String USUARIO_JSON = """
			{"username":"prioste.primero","nombre":"Nombre","apellidos":"Apellidos","cargo":"PRIOSTE_PRIMERO",
			 "email":"prioste@example.com","activo":true,"roles":["PRIOSTE"]}
			""";
	private static final UsuarioDTO USUARIO = new UsuarioDTO(2L, "prioste.primero", "Nombre", "Apellidos",
			CargoUsuarioEnum.PRIOSTE_PRIMERO, "Prioste primero", "prioste@example.com", true, true, false, null,
			Set.of(RolCodigoEnum.PRIOSTE));

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private UsuarioService usuarioService;

	@MockitoBean
	private RolService rolService;

	@Test
	void sinTokenDevuelve401() throws Exception {
		mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest
	@ValueSource(strings = { "PRIOSTE", "MAYORDOMO", "SECRETARIO", "CAPILLER" })
	void soloElAdministradorPuedeGestionarUsuarios(String rol) throws Exception {
		mvc.perform(get("/api/v1/usuarios").with(rol(rol))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/usuarios/2").with(rol(rol))).andExpect(status().isForbidden());
		mvc.perform(conJson(post("/api/v1/usuarios")).with(rol(rol))).andExpect(status().isForbidden());
		mvc.perform(conJson(put("/api/v1/usuarios/2")).with(rol(rol))).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/usuarios/2/restablecer-password").with(rol(rol))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/roles").with(rol(rol))).andExpect(status().isForbidden());
		verifyNoInteractions(usuarioService, rolService);
	}

	@Test
	void listaLosUsuariosSinDatosDeContrasena() throws Exception {
		when(usuarioService.listar()).thenReturn(List.of(USUARIO));

		mvc.perform(get("/api/v1/usuarios").with(rol("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username").value("prioste.primero"))
				.andExpect(jsonPath("$[0].cargoEtiqueta").value("Prioste primero"))
				.andExpect(jsonPath("$[0].roles[0]").value("PRIOSTE"))
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}

	@Test
	void crearDevuelve201ConLaContrasenaTemporal() throws Exception {
		when(usuarioService.crear(any())).thenReturn(new UsuarioCreadoDTO(USUARIO, "Temporal12345"));

		mvc.perform(conJson(post("/api/v1/usuarios")).with(rol("ADMIN")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuario.id").value(2))
				.andExpect(jsonPath("$.passwordTemporal").value("Temporal12345"));
	}

	@Test
	void restablecerDevuelveLaContrasenaTemporal() throws Exception {
		when(usuarioService.restablecerPassword(2L)).thenReturn(new UsuarioCreadoDTO(USUARIO, "Temporal67890"));

		mvc.perform(post("/api/v1/usuarios/2/restablecer-password").with(rol("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passwordTemporal").value("Temporal67890"));
	}

	@Test
	void unaReglaDeNegocioDevuelve409ConElMotivo() throws Exception {
		when(usuarioService.actualizar(eq(1L), any()))
				.thenThrow(new ReglaNegocioException("No se puede: es el único administrador activo"));

		mvc.perform(conJson(put("/api/v1/usuarios/1")).with(rol("ADMIN")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("No se puede: es el único administrador activo"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"username\":\"sin.activo\",\"nombre\":\"N\",\"cargo\":\"CAPILLER\",\"roles\":[\"CAPILLER\"]}",
			"{\"username\":\"sin.roles\",\"nombre\":\"N\",\"cargo\":\"CAPILLER\",\"activo\":true,\"roles\":[]}",
			"{\"username\":\"con espacio\",\"nombre\":\"N\",\"cargo\":\"CAPILLER\",\"activo\":true,\"roles\":[\"CAPILLER\"]}",
			"{\"username\":\"sin.cargo\",\"nombre\":\"N\",\"activo\":true,\"roles\":[\"CAPILLER\"]}" })
	void rechazaDatosNoValidos(String json) throws Exception {
		mvc.perform(put("/api/v1/usuarios/2").contentType(MediaType.APPLICATION_JSON).content(json).with(rol("ADMIN")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(usuarioService);
	}

	@Test
	void noSePuedeBorrarUnUsuario() throws Exception {
		mvc.perform(delete("/api/v1/usuarios/2").with(rol("ADMIN"))).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void losRolesSoloSeConsultan() throws Exception {
		when(rolService.listar()).thenReturn(
				List.of(new RolDTO(1L, RolCodigoEnum.ADMIN, "Administrador", "Gestión técnica", Set.of())));

		mvc.perform(get("/api/v1/roles").with(rol("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].codigo").value("ADMIN"));
		mvc.perform(conJson(put("/api/v1/roles/1")).with(rol("ADMIN"))).andExpect(status().isNotFound());
	}

	private static MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder peticion) {
		return peticion.contentType(MediaType.APPLICATION_JSON).content(USUARIO_JSON);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

}
