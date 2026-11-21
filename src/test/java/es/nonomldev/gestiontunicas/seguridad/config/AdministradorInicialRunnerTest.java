package es.nonomldev.gestiontunicas.seguridad.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;

class AdministradorInicialRunnerTest {

	private static final String PASSWORD = "contrasena-inicial";

	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final RolRepository rolRepository = mock(RolRepository.class);
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
	private final RolEntity rolAdmin = new RolEntity();

	@BeforeEach
	void preparar() {
		rolAdmin.setCodigo(RolCodigoEnum.ADMIN);
		when(rolRepository.findByCodigo(RolCodigoEnum.ADMIN)).thenReturn(Optional.of(rolAdmin));
	}

	@Test
	void creaElAdministradorQueDebeCambiarLaContrasena() {
		runner("administrador", PASSWORD).run(null);

		ArgumentCaptor<UsuarioEntity> guardado = ArgumentCaptor.forClass(UsuarioEntity.class);
		verify(usuarioRepository).save(guardado.capture());
		UsuarioEntity admin = guardado.getValue();
		assertThat(admin.getUsername()).isEqualTo("administrador");
		assertThat(admin.getCargo()).isEqualTo(CargoUsuarioEnum.ADMINISTRADOR);
		assertThat(admin.getRoles()).containsExactly(rolAdmin);
		assertThat(admin.isDebeCambiarPassword()).isTrue();
		assertThat(admin.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
	}

	@Test
	void noHaceNadaSiYaHayUnAdministradorActivo() {
		when(usuarioRepository.existsByActivoTrueAndRoles_Codigo(RolCodigoEnum.ADMIN)).thenReturn(true);

		runner("administrador", PASSWORD).run(null);

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void noCreaNadaSinCredenciales() {
		runner("", "").run(null);

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void noModificaUnUsuarioExistenteConEseNombre() {
		when(usuarioRepository.existsByUsername("administrador")).thenReturn(true);

		runner("administrador", PASSWORD).run(null);

		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void rechazaUnaContrasenaDemasiadoCorta() {
		AdministradorInicialRunner runner = runner("administrador", "1234567");

		assertThatThrownBy(() -> runner.run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ADMIN_PASSWORD");
		verify(usuarioRepository, never()).save(any());
	}

	private AdministradorInicialRunner runner(String username, String password) {
		return new AdministradorInicialRunner(usuarioRepository, rolRepository, passwordEncoder,
				new AdminInicialProperties(username, password));
	}

}
