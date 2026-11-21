package es.nonomldev.gestiontunicas.seguridad.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * SOLO PARA PRUEBAS (perfil dev): crea el usuario {@code admin} con contraseña {@code 1234} y rol ADMIN.
 * <p>
 * Se crea si no existe y se ejecuta antes que {@link AdministradorInicialRunner}. No se ejecuta en producción.
 */
@Slf4j
@Profile("dev")
@Configuration
public class UsuarioPruebasDevConfig {

	private static final String USERNAME = "admin";
	private static final String PASSWORD = "1234";

	@Bean
	@Order(0)
	ApplicationRunner crearUsuarioPruebas(UsuarioRepository usuarioRepository, RolRepository rolRepository,
			PasswordEncoder passwordEncoder, TransactionTemplate transactionTemplate) {
		return args -> transactionTemplate.executeWithoutResult(estado -> {
			if (usuarioRepository.existsByUsername(USERNAME)) {
				return;
			}

			RolEntity rolAdmin = rolRepository.findByCodigo(RolCodigoEnum.ADMIN).orElseGet(() -> {
				RolEntity rol = new RolEntity();
				rol.setCodigo(RolCodigoEnum.ADMIN);
				rol.setNombre("Administrador");
				return rolRepository.save(rol);
			});

			UsuarioEntity admin = new UsuarioEntity();
			admin.setUsername(USERNAME);
			admin.setPasswordHash(passwordEncoder.encode(PASSWORD));
			admin.setNombre("Administrador");
			admin.setCargo(CargoUsuarioEnum.ADMINISTRADOR);
			// Para no tener que cambiar la contraseña cada vez que se arranca en local.
			admin.setDebeCambiarPassword(false);
			admin.getRoles().add(rolAdmin);
			usuarioRepository.save(admin);

			log.warn("Perfil dev: creado el usuario de pruebas '{}' con contraseña '{}'", USERNAME, PASSWORD);
		});
	}

}
