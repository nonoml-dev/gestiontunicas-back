package es.nonomldev.gestiontunicas.seguridad.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.CredencialesInvalidasException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.configuracion.service.ConfiguracionService;
import es.nonomldev.gestiontunicas.seguridad.config.JwtProperties;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.RefreshTokenEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.mapper.UsuarioMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.RefreshTokenRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import es.nonomldev.gestiontunicas.seguridad.service.AuthService;
import es.nonomldev.gestiontunicas.seguridad.service.RegistroAuditoriaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos";
	static final String MENSAJE_BLOQUEADO = "Usuario bloqueado por demasiados intentos fallidos. Inténtalo de nuevo en %d minutos";
	static final String MENSAJE_SESION_CADUCADA = "La sesión ha caducado. Vuelve a iniciar sesión";

	/** Parámetros de la tabla configuracion; si no existen se usan los valores por defecto. */
	static final String CLAVE_INTENTOS_MAXIMOS = "seguridad.intentos_maximos";
	static final String CLAVE_MINUTOS_BLOQUEO = "seguridad.minutos_bloqueo";
	static final int INTENTOS_MAXIMOS_POR_DEFECTO = 5;
	static final int MINUTOS_BLOQUEO_POR_DEFECTO = 15;

	private static final String EMISOR = "gestiontunicas";
	private static final int BYTES_REFRESH_TOKEN = 32;
	/** Límite de BCrypt. */
	private static final int BYTES_MAXIMOS_PASSWORD = 72;

	private final UsuarioRepository usuarioRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final RegistroAuditoriaService registroAuditoriaService;
	private final ConfiguracionService configuracionService;
	private final UsuarioMapper usuarioMapper;
	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	/** Sin rollback al rechazar: el contador de intentos fallidos y el bloqueo deben guardarse. */
	@Override
	@Transactional(noRollbackFor = CredencialesInvalidasException.class)
	public LoginDTO login(LoginRequest request, String ip) {
		String username = request.username().trim();
		Instant ahora = Instant.now(clock);
		UsuarioEntity usuario = usuarioRepository.findByUsername(username).orElse(null);

		if (usuario != null && usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
			registroAuditoriaService.registrar(username, ResultadoAccesoEnum.BLOQUEADO, ip);
			throw new CredencialesInvalidasException(mensajeBloqueado(usuario.getBloqueadoHasta(), ahora));
		}

		// Mismo mensaje si no existe, está inactivo o la contraseña no coincide: no se revela qué falló.
		if (usuario == null || !usuario.isActivo()
				|| !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
			registroAuditoriaService.registrar(username, ResultadoAccesoEnum.FALLIDO, ip);
			if (usuario != null && usuario.isActivo()) {
				anotarIntentoFallido(usuario, ahora);
			}
			throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES);
		}

		usuario.setIntentosFallidos(0);
		usuario.setBloqueadoHasta(null);
		usuario.setUltimoAcceso(ahora);
		registroAuditoriaService.registrar(username, ResultadoAccesoEnum.CORRECTO, ip);
		return abrirSesion(usuario, ahora);
	}

	/** Sin rollback al rechazar: si se reutiliza un token ya revocado se cierran todas las sesiones del usuario. */
	@Override
	@Transactional(noRollbackFor = CredencialesInvalidasException.class)
	public LoginDTO refrescar(String refreshToken) {
		Instant ahora = Instant.now(clock);
		RefreshTokenEntity token = refreshTokenRepository.findByTokenHash(hash(refreshToken))
				.orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_SESION_CADUCADA));

		if (token.isRevocado()) {
			// Un token ya usado vuelve a aparecer: puede haberlo copiado otra persona. Se cierra todo.
			refreshTokenRepository.revocarTodosDeUsuario(token.getUsuario().getId());
			throw new CredencialesInvalidasException(MENSAJE_SESION_CADUCADA);
		}
		UsuarioEntity usuario = token.getUsuario();
		boolean bloqueado = usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora);
		if (!token.getExpiraEn().isAfter(ahora) || !usuario.isActivo() || bloqueado) {
			token.setRevocado(true);
			throw new CredencialesInvalidasException(MENSAJE_SESION_CADUCADA);
		}

		token.setRevocado(true);
		return abrirSesion(usuario, ahora);
	}

	@Override
	@Transactional
	public void cerrarSesion(String refreshToken) {
		refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(token -> token.setRevocado(true));
	}

	/** Sin rollback al rechazar: el contador de intentos fallidos debe guardarse. */
	@Override
	@Transactional(noRollbackFor = ReglaNegocioException.class)
	public LoginDTO cambiarPassword(String username, CambioPasswordRequest request) {
		Instant ahora = Instant.now(clock);
		UsuarioEntity usuario = usuarioRepository.findByUsername(username)
				.orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_SESION_CADUCADA));
		boolean bloqueado = usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora);
		if (!usuario.isActivo() || bloqueado) {
			throw new CredencialesInvalidasException(MENSAJE_SESION_CADUCADA);
		}
		if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
			anotarIntentoFallido(usuario, ahora);
			throw new ReglaNegocioException("La contraseña actual no es correcta");
		}
		if (request.passwordNueva().getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_PASSWORD) {
			throw new ReglaNegocioException("La contraseña nueva es demasiado larga");
		}
		if (passwordEncoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
			throw new ReglaNegocioException("La contraseña nueva tiene que ser distinta de la actual");
		}
		usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
		usuario.setDebeCambiarPassword(false);
		usuario.setIntentosFallidos(0);
		refreshTokenRepository.revocarTodosDeUsuario(usuario.getId());
		return abrirSesion(usuario, ahora);
	}

	private void anotarIntentoFallido(UsuarioEntity usuario, Instant ahora) {
		int intentos = usuario.getIntentosFallidos() + 1;
		int maximo = configuracionService.entero(CLAVE_INTENTOS_MAXIMOS).orElse(INTENTOS_MAXIMOS_POR_DEFECTO);
		if (intentos >= maximo) {
			int minutos = configuracionService.entero(CLAVE_MINUTOS_BLOQUEO).orElse(MINUTOS_BLOQUEO_POR_DEFECTO);
			usuario.setBloqueadoHasta(ahora.plus(Duration.ofMinutes(minutos)));
			// Al terminar el bloqueo vuelve a tener todos los intentos.
			intentos = 0;
		}
		usuario.setIntentosFallidos(intentos);
	}

	private static String mensajeBloqueado(Instant bloqueadoHasta, Instant ahora) {
		long minutos = Math.max(1, (Duration.between(ahora, bloqueadoHasta).toSeconds() + 59) / 60);
		return MENSAJE_BLOQUEADO.formatted(minutos);
	}

	private LoginDTO abrirSesion(UsuarioEntity usuario, Instant ahora) {
		Instant expiraEn = ahora.plus(jwtProperties.duracion());
		Instant refreshExpiraEn = ahora.plus(jwtProperties.duracionRefresh());
		String refreshToken = nuevoRefreshToken();

		RefreshTokenEntity token = new RefreshTokenEntity();
		token.setUsuario(usuario);
		token.setTokenHash(hash(refreshToken));
		token.setCreadoEn(ahora);
		token.setExpiraEn(refreshExpiraEn);
		refreshTokenRepository.save(token);

		return new LoginDTO(emitirToken(usuario, ahora, expiraEn), "Bearer", expiraEn, refreshToken, refreshExpiraEn,
				usuarioMapper.toDTO(usuario));
	}

	private String emitirToken(UsuarioEntity usuario, Instant ahora, Instant expiraEn) {
		List<String> roles = usuario.getRoles().stream().map(rol -> rol.getCodigo().name()).sorted().toList();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(EMISOR)
				.subject(usuario.getUsername())
				.issuedAt(ahora)
				.expiresAt(expiraEn)
				.claim(SecurityConfig.CLAIM_ROLES, roles);
		if (usuario.isDebeCambiarPassword()) {
			claims.claim(SecurityConfig.CLAIM_DEBE_CAMBIAR_PASSWORD, true);
		}
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims.build())).getTokenValue();
	}

	private String nuevoRefreshToken() {
		byte[] bytes = new byte[BYTES_REFRESH_TOKEN];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** En la base de datos solo se guarda el SHA-256 del refresh token. */
	private static String hash(String refreshToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}

}
