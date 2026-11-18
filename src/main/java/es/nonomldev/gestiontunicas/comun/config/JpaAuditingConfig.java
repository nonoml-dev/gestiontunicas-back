package es.nonomldev.gestiontunicas.comun.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Activa la auditoría de JPA para {@code BaseAuditableEntity}.
 * El usuario se toma del contexto de seguridad; si no hay nadie autenticado
 * (tareas programadas, importaciones de arranque...), se guarda "sistema".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	static final String USUARIO_SISTEMA = "sistema";

	@Bean
	AuditorAware<String> auditorAware() {
		return () -> {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
				return Optional.of(USUARIO_SISTEMA);
			}
			return Optional.of(auth.getName());
		};
	}

}
