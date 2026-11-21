package es.nonomldev.gestiontunicas.seguridad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import es.nonomldev.gestiontunicas.comun.exception.CredencialesInvalidasException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.configuracion.service.ConfiguracionService;
import es.nonomldev.gestiontunicas.seguridad.config.JwtProperties;
import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RefreshTokenEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.mapper.UsuarioMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.RefreshTokenRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import es.nonomldev.gestiontunicas.seguridad.service.RegistroAuditoriaService;

class AuthServiceImplTest {

	private static final String SECRETO = "clave-de-pruebas-de-al-menos-32-caracteres";
	private static final Instant AHORA = Instant.parse("2027-03-20T10:00:00Z");
	private static final String IP = "127.0.0.1";

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final RegistroAuditoriaService auditoria = mock(RegistroAuditoriaService.class);
	private final ConfiguracionService configuracion = mock(ConfiguracionService.class);
	private final UsuarioMapper usuarioMapper = mock(UsuarioMapper.class);
	private final SecretKeySpec clave = new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

	/** Refresh tokens "guardados", para poder buscarlos por su hash como haría la base de datos. */
	private final List<RefreshTokenEntity> guardados = new ArrayList<>();

	private AuthServiceImpl service;
	private UsuarioEntity admin;

	@BeforeEach
	void preparar() {
		service = new AuthServiceImpl(usuarioRepository, refreshTokenRepository, passwordEncoder, auditoria,
				configuracion, usuarioMapper, new NimbusJwtEncoder(new ImmutableSecret<>(clave)),
				new JwtProperties(SECRETO, Duration.ofMinutes(15), Duration.ofHours(12)),
				Clock.fixed(AHORA, ZoneId.of("Europe/Madrid")));

		RolEntity rol = new RolEntity();
		rol.setCodigo(RolCodigoEnum.ADMIN);
		admin = new UsuarioEntity();
		admin.setUsername("admin");
		admin.setPasswordHash(passwordEncoder.encode("1234"));
		admin.setNombre("Administrador");
		admin.setCargo(CargoUsuarioEnum.ADMINISTRADOR);
		admin.getRoles().add(rol);

		when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
		when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());
		when(configuracion.entero(anyString())).thenReturn(Optional.empty());
		when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
			guardados.add(inv.getArgument(0));
			return inv.getArgument(0);
		});
		when(refreshTokenRepository.findByTokenHash(anyString())).thenAnswer(inv -> guardados.stream()
				.filter(t -> t.getTokenHash().equals(inv.getArgument(0))).findFirst());
	}

	@Test
	void loginCorrectoEmiteTokenConRolesYRegistraElAcceso() {
		admin.setIntentosFallidos(3);

		LoginDTO sesion = service.login(new LoginRequest(" admin ", "1234"), IP);

		Jwt jwt = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build()
				.decode(sesion.accessToken());
		assertThat(jwt.getSubject()).isEqualTo("admin");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
		assertThat(sesion.expiraEn()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
		assertThat(sesion.refreshExpiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(12)));
		assertThat(sesion.refreshToken()).isNotBlank();
		// En la base de datos solo se guarda el hash.
		assertThat(guardados).singleElement().satisfies(t -> assertThat(t.getTokenHash()).isNotEqualTo(sesion.refreshToken()));
		assertThat(admin.getIntentosFallidos()).isZero();
		assertThat(admin.getUltimoAcceso()).isEqualTo(AHORA);
		verify(auditoria).registrar("admin", ResultadoAccesoEnum.CORRECTO, IP);
	}

	@Test
	void usuarioInexistenteYContrasenaIncorrectaDanElMismoMensaje() {
		assertThatThrownBy(() -> service.login(new LoginRequest("nadie", "1234"), IP))
				.isInstanceOf(CredencialesInvalidasException.class).hasMessage(AuthServiceImpl.MENSAJE_CREDENCIALES);
		assertThatThrownBy(() -> service.login(new LoginRequest("admin", "mala"), IP))
				.isInstanceOf(CredencialesInvalidasException.class).hasMessage(AuthServiceImpl.MENSAJE_CREDENCIALES);
		verify(auditoria).registrar("nadie", ResultadoAccesoEnum.FALLIDO, IP);
		verify(auditoria).registrar("admin", ResultadoAccesoEnum.FALLIDO, IP);
		assertThat(admin.getIntentosFallidos()).isEqualTo(1);
	}

	@Test
	void alQuintoIntentoFallidoSeBloqueaQuinceMinutosAunqueLuegoAcierte() {
		for (int i = 0; i < AuthServiceImpl.INTENTOS_MAXIMOS_POR_DEFECTO; i++) {
			assertThatThrownBy(() -> service.login(new LoginRequest("admin", "mala"), IP))
					.isInstanceOf(CredencialesInvalidasException.class);
		}
		assertThat(admin.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));

		assertThatThrownBy(() -> service.login(new LoginRequest("admin", "1234"), IP))
				.isInstanceOf(CredencialesInvalidasException.class).hasMessageContaining("bloqueado")
				.hasMessageContaining("15 minutos");
		verify(auditoria).registrar("admin", ResultadoAccesoEnum.BLOQUEADO, IP);
	}

	@Test
	void respetaLosIntentosYMinutosDeLaConfiguracion() {
		when(configuracion.entero(AuthServiceImpl.CLAVE_INTENTOS_MAXIMOS)).thenReturn(Optional.of(2));
		when(configuracion.entero(AuthServiceImpl.CLAVE_MINUTOS_BLOQUEO)).thenReturn(Optional.of(30));

		for (int i = 0; i < 2; i++) {
			assertThatThrownBy(() -> service.login(new LoginRequest("admin", "mala"), IP))
					.isInstanceOf(CredencialesInvalidasException.class);
		}
		assertThat(admin.getBloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(30)));
	}

	@Test
	void usuarioInactivoNoEntra() {
		admin.setActivo(false);
		assertThatThrownBy(() -> service.login(new LoginRequest("admin", "1234"), IP))
				.isInstanceOf(CredencialesInvalidasException.class).hasMessage(AuthServiceImpl.MENSAJE_CREDENCIALES);
	}

	@Test
	void refrescarCambiaElRefreshTokenYRevocaElAnterior() {
		LoginDTO login = service.login(new LoginRequest("admin", "1234"), IP);
		guardados.getFirst().setUsuario(admin);

		LoginDTO renovada = service.refrescar(login.refreshToken());

		assertThat(renovada.refreshToken()).isNotEqualTo(login.refreshToken());
		assertThat(guardados.getFirst().isRevocado()).isTrue();
		assertThat(guardados.get(1).isRevocado()).isFalse();
	}

	@Test
	void reutilizarUnRefreshTokenYaUsadoCierraTodasLasSesiones() {
		LoginDTO login = service.login(new LoginRequest("admin", "1234"), IP);
		guardados.getFirst().setUsuario(admin);
		service.refrescar(login.refreshToken());

		assertThatThrownBy(() -> service.refrescar(login.refreshToken()))
				.isInstanceOf(CredencialesInvalidasException.class).hasMessage(AuthServiceImpl.MENSAJE_SESION_CADUCADA);
		verify(refreshTokenRepository).revocarTodosDeUsuario(admin.getId());
	}

	@Test
	void refreshTokenDesconocidoOCaducadoSeRechaza() {
		assertThatThrownBy(() -> service.refrescar("inventado"))
				.isInstanceOf(CredencialesInvalidasException.class);

		LoginDTO login = service.login(new LoginRequest("admin", "1234"), IP);
		guardados.getFirst().setUsuario(admin);
		guardados.getFirst().setExpiraEn(AHORA.minusSeconds(1));
		assertThatThrownBy(() -> service.refrescar(login.refreshToken()))
				.isInstanceOf(CredencialesInvalidasException.class);
	}

	@Test
	void cerrarSesionRevocaElRefreshToken() {
		LoginDTO login = service.login(new LoginRequest("admin", "1234"), IP);

		service.cerrarSesion(login.refreshToken());
		service.cerrarSesion("inventado");

		assertThat(guardados.getFirst().isRevocado()).isTrue();
	}

	@Test
	void elTokenIndicaSiLaContrasenaEstaPendienteDeCambiar() {
		admin.setDebeCambiarPassword(true);
		assertThat(claims(service.login(new LoginRequest("admin", "1234"), IP)).getClaimAsBoolean("debe_cambiar_password"))
				.isTrue();

		admin.setDebeCambiarPassword(false);
		assertThat(claims(service.login(new LoginRequest("admin", "1234"), IP)).hasClaim("debe_cambiar_password"))
				.isFalse();
	}

	@Test
	void cambiarLaContrasenaCierraLasSesionesYAbreUnaSinPendiente() {
		admin.setDebeCambiarPassword(true);
		admin.setIntentosFallidos(2);

		LoginDTO sesion = service.cambiarPassword("admin", new CambioPasswordRequest("1234", "contrasena-nueva"));

		assertThat(passwordEncoder.matches("contrasena-nueva", admin.getPasswordHash())).isTrue();
		assertThat(admin.isDebeCambiarPassword()).isFalse();
		assertThat(admin.getIntentosFallidos()).isZero();
		assertThat(claims(sesion).hasClaim("debe_cambiar_password")).isFalse();
		verify(refreshTokenRepository).revocarTodosDeUsuario(admin.getId());
	}

	@Test
	void conLaContrasenaActualIncorrectaNoCambiaYCuentaComoIntento() {
		String hash = admin.getPasswordHash();

		assertThatThrownBy(() -> service.cambiarPassword("admin", new CambioPasswordRequest("mala", "contrasena-nueva")))
				.isInstanceOf(ReglaNegocioException.class).hasMessageContaining("actual no es correcta");

		assertThat(admin.getPasswordHash()).isEqualTo(hash);
		assertThat(admin.getIntentosFallidos()).isEqualTo(1);
	}

	@Test
	void laContrasenaNuevaTieneQueSerDistinta() {
		admin.setPasswordHash(passwordEncoder.encode("contrasena-actual"));

		assertThatThrownBy(() -> service.cambiarPassword("admin",
				new CambioPasswordRequest("contrasena-actual", "contrasena-actual")))
				.isInstanceOf(ReglaNegocioException.class).hasMessageContaining("distinta");
		assertThat(admin.isDebeCambiarPassword()).isTrue();
	}

	@Test
	void rechazaUnaContrasenaDeMasDe72Bytes() {
		String larga = "ñ".repeat(40);

		assertThatThrownBy(() -> service.cambiarPassword("admin", new CambioPasswordRequest("1234", larga)))
				.isInstanceOf(ReglaNegocioException.class).hasMessageContaining("demasiado larga");
	}

	@Test
	void unUsuarioBloqueadoOInactivoNoPuedeCambiarla() {
		admin.setBloqueadoHasta(AHORA.plusSeconds(60));
		assertThatThrownBy(() -> service.cambiarPassword("admin", new CambioPasswordRequest("1234", "contrasena-nueva")))
				.isInstanceOf(CredencialesInvalidasException.class);

		admin.setBloqueadoHasta(null);
		admin.setActivo(false);
		assertThatThrownBy(() -> service.cambiarPassword("admin", new CambioPasswordRequest("1234", "contrasena-nueva")))
				.isInstanceOf(CredencialesInvalidasException.class);
	}

	private Jwt claims(LoginDTO sesion) {
		return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build().decode(sesion.accessToken());
	}

}
