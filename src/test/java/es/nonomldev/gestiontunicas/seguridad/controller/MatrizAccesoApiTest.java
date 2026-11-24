package es.nonomldev.gestiontunicas.seguridad.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.hermano.service.HermanoService;
import es.nonomldev.gestiontunicas.importacion.service.ImportacionHermanosService;
import es.nonomldev.gestiontunicas.importacion.service.ImportacionTunicasService;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;
import es.nonomldev.gestiontunicas.seguridad.service.AuthService;
import es.nonomldev.gestiontunicas.seguridad.service.RolService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioService;
import es.nonomldev.gestiontunicas.tunica.service.ReparacionService;
import es.nonomldev.gestiontunicas.tunica.service.TunicaService;

/**
 * Acceso permitido y denegado de cada endpoint para cada rol, según la matriz del análisis §3.
 * Un endpoint nuevo hace fallar {@link #todosLosEndpointsEstanEnLaMatriz()} hasta declarar aquí sus roles.
 * Los cuerpos son válidos a propósito: la validación corre antes que {@code @PreAuthorize}.
 */
@WebMvcTest
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class MatrizAccesoApiTest {

	private static final Set<String> ROLES = Set.of("ADMIN", "PRIOSTE", "MAYORDOMO", "SECRETARIO", "CAPILLER");
	private static final Set<String> SOLO_ADMIN = Set.of("ADMIN");
	private static final Set<String> GESTION_HERMANOS = Set.of("ADMIN", "PRIOSTE", "MAYORDOMO", "SECRETARIO");
	private static final Set<String> GESTION_TUNICAS = Set.of("ADMIN", "PRIOSTE");

	private static final String USUARIO = """
			{"username":"usuario.prueba","nombre":"Nombre","apellidos":"Apellidos","cargo":"CAPILLER",
			 "email":null,"activo":true,"roles":["CAPILLER"]}
			""";
	private static final String HERMANO = "{\"nombre\":\"Nombre\",\"apellidos\":\"Apellidos\"}";
	private static final String CONTACTO = "{\"telefono1\":\"600000000\"}";
	private static final String DECISION = "{\"decision\":\"IGNORAR\"}";
	private static final String TUNICA = "{\"codigo\":\"42\",\"medidaCm\":158,\"dobladilloCm\":6}";
	private static final String MOTIVO = "{\"motivo\":\"Rota sin arreglo\"}";
	private static final String REPARACION = """
			{"tipoPieza":"CAPA","tipo":"REPARACION","descripcion":"Escudo JHS despegado","fechaInicio":"2026-11-24"}
			""";
	private static final String CAMBIO_PASSWORD = "{\"passwordActual\":\"temporal-1234\",\"passwordNueva\":\"contrasena-nueva\"}";
	private static final String LOGIN = "{\"username\":\"usuario.prueba\",\"password\":\"clave-de-prueba\"}";
	private static final String REFRESH = "{\"refreshToken\":\"refresh-de-prueba\"}";
	private static final String SUBIDA_EXCEL = "POST /api/v1/hermanos/importaciones";
	private static final String SUBIDA_INVENTARIO = "POST /api/v1/tunicas/importaciones";
	private static final String CAMBIAR_PASSWORD = "POST /api/v1/auth/cambiar-password";

	private static final List<Endpoint> PROTEGIDOS = List.of(
			new Endpoint(CAMBIAR_PASSWORD, 200, CAMBIO_PASSWORD, ROLES),

			new Endpoint("GET /api/v1/usuarios", 200, null, SOLO_ADMIN),
			new Endpoint("GET /api/v1/usuarios/{id}", 200, null, SOLO_ADMIN),
			new Endpoint("POST /api/v1/usuarios", 201, USUARIO, SOLO_ADMIN),
			new Endpoint("PUT /api/v1/usuarios/{id}", 200, USUARIO, SOLO_ADMIN),
			new Endpoint("POST /api/v1/usuarios/{id}/restablecer-password", 200, null, SOLO_ADMIN),
			new Endpoint("GET /api/v1/roles", 200, null, SOLO_ADMIN),

			new Endpoint("GET /api/v1/hermanos", 200, null, ROLES),
			new Endpoint("GET /api/v1/hermanos/criterio-edad", 200, null, ROLES),
			new Endpoint("GET /api/v1/hermanos/{id}", 200, null, ROLES),
			new Endpoint("POST /api/v1/hermanos", 201, HERMANO, GESTION_HERMANOS),
			new Endpoint("PUT /api/v1/hermanos/{id}", 200, CONTACTO, GESTION_HERMANOS),
			new Endpoint("DELETE /api/v1/hermanos/{id}", 204, null, GESTION_HERMANOS),

			new Endpoint(SUBIDA_EXCEL, 201, null, GESTION_HERMANOS),
			new Endpoint("GET /api/v1/hermanos/importaciones/{id}", 200, null, GESTION_HERMANOS),
			new Endpoint("GET /api/v1/hermanos/importaciones/{id}/filas", 200, null, GESTION_HERMANOS),
			new Endpoint("PUT /api/v1/hermanos/importaciones/filas/{filaId}/decision", 200, DECISION, GESTION_HERMANOS),
			new Endpoint("POST /api/v1/hermanos/importaciones/{id}/confirmar", 200, null, GESTION_HERMANOS),
			new Endpoint("POST /api/v1/hermanos/importaciones/{id}/cancelar", 200, null, GESTION_HERMANOS),
			new Endpoint("GET /api/v1/hermanos/importaciones/plantilla", 200, null, GESTION_HERMANOS),

			new Endpoint("GET /api/v1/tunicas", 200, null, ROLES),
			new Endpoint("GET /api/v1/tunicas/propuesta", 200, null, ROLES),
			new Endpoint("GET /api/v1/tunicas/aviso-codigo", 200, null, GESTION_TUNICAS),
			new Endpoint("GET /api/v1/tunicas/{id}", 200, null, ROLES),
			new Endpoint("POST /api/v1/tunicas", 201, TUNICA, GESTION_TUNICAS),
			new Endpoint("PUT /api/v1/tunicas/{id}", 200, TUNICA, GESTION_TUNICAS),
			new Endpoint("POST /api/v1/tunicas/{id}/baja", 200, MOTIVO, GESTION_TUNICAS),

			new Endpoint("GET /api/v1/tunicas/{tunicaId}/reparaciones", 200, null, ROLES),
			new Endpoint("POST /api/v1/tunicas/{tunicaId}/reparaciones", 201, REPARACION, GESTION_TUNICAS),
			new Endpoint("PUT /api/v1/tunicas/{tunicaId}/reparaciones/{id}", 200, REPARACION, GESTION_TUNICAS),

			new Endpoint(SUBIDA_INVENTARIO, 201, null, GESTION_TUNICAS),
			new Endpoint("GET /api/v1/tunicas/importaciones/{id}", 200, null, GESTION_TUNICAS),
			new Endpoint("GET /api/v1/tunicas/importaciones/{id}/filas", 200, null, GESTION_TUNICAS),
			new Endpoint("PUT /api/v1/tunicas/importaciones/filas/{filaId}/decision", 200, DECISION, GESTION_TUNICAS),
			new Endpoint("POST /api/v1/tunicas/importaciones/{id}/confirmar", 200, null, GESTION_TUNICAS),
			new Endpoint("POST /api/v1/tunicas/importaciones/{id}/cancelar", 200, null, GESTION_TUNICAS),
			new Endpoint("GET /api/v1/tunicas/importaciones/plantilla", 200, null, GESTION_TUNICAS));

	/** Parámetros obligatorios de algunos GET, para que la petición sea válida. */
	private static final Map<String, String> PARAMETROS = Map.of(
			"GET /api/v1/tunicas/propuesta", "?alturaCm=160",
			"GET /api/v1/tunicas/aviso-codigo", "?codigo=42");

	private static final List<Endpoint> PUBLICOS = List.of(
			new Endpoint("POST /api/v1/auth/login", 200, LOGIN, Set.of()),
			new Endpoint("POST /api/v1/auth/refresh", 200, REFRESH, Set.of()),
			new Endpoint("POST /api/v1/auth/logout", 204, REFRESH, Set.of()));

	@Autowired
	private MockMvc mvc;

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping rutas;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private UsuarioService usuarioService;

	@MockitoBean
	private RolService rolService;

	@MockitoBean
	private HermanoService hermanoService;

	@MockitoBean
	private ImportacionHermanosService importacionService;

	@MockitoBean
	private TunicaService tunicaService;

	@MockitoBean
	private ImportacionTunicasService importacionTunicasService;

	@MockitoBean
	private ReparacionService reparacionService;

	@Test
	void todosLosEndpointsEstanEnLaMatriz() {
		Set<String> existentes = rutas.getHandlerMethods().keySet().stream()
				.flatMap(info -> info.getPatternValues().stream()
						.filter(ruta -> ruta.startsWith("/api/"))
						.flatMap(ruta -> info.getMethodsCondition().getMethods().stream()
								.map(metodo -> metodo + " " + ruta)))
				.collect(Collectors.toCollection(TreeSet::new));
		Set<String> declarados = Stream.concat(PROTEGIDOS.stream(), PUBLICOS.stream())
				.map(Endpoint::clave)
				.collect(Collectors.toCollection(TreeSet::new));

		assertThat(existentes).isEqualTo(declarados);
	}

	@ParameterizedTest(name = "{0} con {1}")
	@MethodSource("endpointYRol")
	void cadaRolTieneElAccesoDeLaMatriz(Endpoint endpoint, String rol) throws Exception {
		boolean permitido = endpoint.roles().contains(rol);

		mvc.perform(peticion(endpoint).with(token(false, rol)))
				.andExpect(status().is(permitido ? endpoint.exito() : 403));

		if (!permitido) {
			sinLlegarAlServicio();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("protegidos")
	void sinTokenDevuelve401(Endpoint endpoint) throws Exception {
		mvc.perform(peticion(endpoint)).andExpect(status().isUnauthorized());
		sinLlegarAlServicio();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("protegidos")
	void conUnTokenNoValidoDevuelve401(Endpoint endpoint) throws Exception {
		mvc.perform(peticion(endpoint).header(HttpHeaders.AUTHORIZATION, "Bearer token-no-valido"))
				.andExpect(status().isUnauthorized());
		sinLlegarAlServicio();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("protegidos")
	void conLaContrasenaPendienteSoloSePuedeCambiar(Endpoint endpoint) throws Exception {
		boolean esElCambio = CAMBIAR_PASSWORD.equals(endpoint.clave());

		mvc.perform(peticion(endpoint).with(token(true, ROLES.toArray(String[]::new))))
				.andExpect(status().is(esElCambio ? endpoint.exito() : 403));

		if (!esElCambio) {
			sinLlegarAlServicio();
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("publicos")
	void losEndpointsPublicosNoPidenToken(Endpoint endpoint) throws Exception {
		mvc.perform(peticion(endpoint)).andExpect(status().is(endpoint.exito()));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("protegidos")
	void conVariosRolesValeElDeMayorAcceso(Endpoint endpoint) throws Exception {
		boolean permitido = endpoint.roles().contains("CAPILLER") || endpoint.roles().contains("SECRETARIO");

		mvc.perform(peticion(endpoint).with(token(false, "CAPILLER", "SECRETARIO")))
				.andExpect(status().is(permitido ? endpoint.exito() : 403));
	}

	private void sinLlegarAlServicio() {
		verifyNoInteractions(authService, usuarioService, rolService, hermanoService, importacionService, tunicaService,
				importacionTunicasService, reparacionService);
	}

	private static Stream<Arguments> endpointYRol() {
		return PROTEGIDOS.stream().flatMap(endpoint -> ROLES.stream().sorted().map(rol -> Arguments.of(endpoint, rol)));
	}

	private static Stream<Endpoint> protegidos() {
		return PROTEGIDOS.stream();
	}

	private static Stream<Endpoint> publicos() {
		return PUBLICOS.stream();
	}

	private static AbstractMockHttpServletRequestBuilder<?> peticion(Endpoint endpoint) {
		String url = endpoint.ruta().replace("{id}", "1").replace("{filaId}", "1").replace("{tunicaId}", "1")
				+ PARAMETROS.getOrDefault(endpoint.clave(), "");
		if (SUBIDA_EXCEL.equals(endpoint.clave()) || SUBIDA_INVENTARIO.equals(endpoint.clave())) {
			return multipart(url).file(new MockMultipartFile("fichero", "fichero.xlsx",
					MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[] { 1 }));
		}
		var peticion = request(HttpMethod.valueOf(endpoint.metodo()), url);
		return endpoint.cuerpo() == null ? peticion
				: peticion.contentType(MediaType.APPLICATION_JSON).content(endpoint.cuerpo());
	}

	private static RequestPostProcessor token(boolean passwordPendiente, String... roles) {
		GrantedAuthority[] autoridades = Arrays.stream(roles)
				.map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
				.toArray(GrantedAuthority[]::new);
		return jwt().authorities(autoridades).jwt(token -> {
			token.subject("usuario.prueba");
			if (passwordPendiente) {
				token.claim(SecurityConfig.CLAIM_DEBE_CAMBIAR_PASSWORD, true);
			}
		});
	}

	/** {@code clave} es "MÉTODO /ruta"; {@code exito}, el código con acceso; {@code roles}, quién lo tiene. */
	private record Endpoint(String clave, int exito, String cuerpo, Set<String> roles) {

		String metodo() {
			return clave.substring(0, clave.indexOf(' '));
		}

		String ruta() {
			return clave.substring(clave.indexOf(' ') + 1);
		}

		@Override
		public String toString() {
			return clave;
		}
	}

}
