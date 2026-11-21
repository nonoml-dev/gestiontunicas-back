package es.nonomldev.gestiontunicas.seguridad.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * API sin estado protegida con JWT (HS256). Solo son públicos el login, la renovación y el cierre de sesión.
 * Cada petición lleva la cabecera {@code Authorization: Bearer <token>}.
 * Los permisos por rol se comprueban en los controladores con {@code @PreAuthorize}.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ JwtProperties.class, AdminInicialProperties.class })
public class SecurityConfig {

	/** Claim del token con los códigos de rol del usuario (ADMIN, PRIOSTE...). */
	public static final String CLAIM_ROLES = "roles";

	/** Claim presente mientras el usuario tenga pendiente cambiar su contraseña. */
	public static final String CLAIM_DEBE_CAMBIAR_PASSWORD = "debe_cambiar_password";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout")
							.permitAll()
						.requestMatchers("/error", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/cambiar-password").authenticated()
						.anyRequest().access(sinCambioDePasswordPendiente()))
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.build();
	}

	/** Con la contraseña pendiente de cambiar, el token solo sirve para cambiarla. */
	private static AuthorizationManager<RequestAuthorizationContext> sinCambioDePasswordPendiente() {
		return (autenticacion, contexto) -> {
			Authentication usuario = autenticacion.get();
			boolean autenticado = usuario != null && usuario.isAuthenticated()
					&& !(usuario instanceof AnonymousAuthenticationToken);
			boolean pendiente = usuario instanceof JwtAuthenticationToken jwt
					&& Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean(CLAIM_DEBE_CAMBIAR_PASSWORD));
			return new AuthorizationDecision(autenticado && !pendiente);
		};
	}

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Los roles del token pasan a ser autoridades ROLE_ADMIN, ROLE_PRIOSTE... */
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
		roles.setAuthoritiesClaimName(CLAIM_ROLES);
		roles.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(roles);
		return converter;
	}

}
