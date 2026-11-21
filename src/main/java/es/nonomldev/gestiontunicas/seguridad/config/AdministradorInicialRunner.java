package es.nonomldev.gestiontunicas.seguridad.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el primer administrador de una instalación nueva con ADMIN_USERNAME y ADMIN_PASSWORD.
 * Si ya hay un administrador activo no hace nada, así que nunca cambia un usuario ni una contraseña existentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdministradorInicialRunner implements ApplicationRunner {

	private static final int LONGITUD_MINIMA_PASSWORD = 8;

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminInicialProperties properties;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.existsByActivoTrueAndRoles_Codigo(RolCodigoEnum.ADMIN)) {
			return;
		}
		if (!properties.definidas()) {
			log.warn("No hay ningún administrador activo. Define ADMIN_USERNAME y ADMIN_PASSWORD y reinicia la aplicación");
			return;
		}
		if (properties.password().length() < LONGITUD_MINIMA_PASSWORD) {
			throw new IllegalStateException(
					"ADMIN_PASSWORD debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
		}
		String username = properties.username().trim();
		if (usuarioRepository.existsByUsername(username)) {
			log.error("No se crea el administrador: ya existe el usuario '{}' y no es administrador activo", username);
			return;
		}
		RolEntity rolAdmin = rolRepository.findByCodigo(RolCodigoEnum.ADMIN)
				.orElseThrow(() -> new IllegalStateException("No existe el rol ADMIN"));

		UsuarioEntity admin = new UsuarioEntity();
		admin.setUsername(username);
		admin.setPasswordHash(passwordEncoder.encode(properties.password()));
		admin.setNombre("Administrador");
		admin.setCargo(CargoUsuarioEnum.ADMINISTRADOR);
		admin.setDebeCambiarPassword(true);
		admin.getRoles().add(rolAdmin);
		usuarioRepository.save(admin);
		log.info("Creado el primer administrador '{}'", username);
	}

}
