package es.nonomldev.gestiontunicas.seguridad.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.mapper.UsuarioMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.RefreshTokenRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;

class UsuarioServiceImplTest {

	private static final Long ID = 1L;

	private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
	private final RolRepository rolRepository = mock(RolRepository.class);
	private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
	private final UsuarioMapper usuarioMapper = mock(UsuarioMapper.class);
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

	private final RolEntity rolAdmin = rol(RolCodigoEnum.ADMIN);
	private final RolEntity rolPrioste = rol(RolCodigoEnum.PRIOSTE);

	private UsuarioServiceImpl service;
	private UsuarioEntity usuario;

	@BeforeEach
	void preparar() {
		service = new UsuarioServiceImpl(usuarioRepository, rolRepository, refreshTokenRepository, usuarioMapper,
				passwordEncoder);

		usuario = new UsuarioEntity();
		ReflectionTestUtils.setField(usuario, "id", ID);
		usuario.setUsername("admin.junta");
		usuario.setActivo(true);
		usuario.setRoles(new HashSet<>(Set.of(rolAdmin)));
		when(usuarioRepository.findById(ID)).thenReturn(Optional.of(usuario));

		when(rolRepository.findByCodigoIn(any())).thenAnswer(llamada -> {
			Set<RolCodigoEnum> codigos = llamada.getArgument(0);
			return List.of(rolAdmin, rolPrioste).stream().filter(r -> codigos.contains(r.getCodigo())).toList();
		});
		doAnswer(llamada -> {
			UsuarioRequest request = llamada.getArgument(0);
			UsuarioEntity destino = llamada.getArgument(1);
			destino.setUsername(request.username());
			destino.setActivo(request.activo());
			return null;
		}).when(usuarioMapper).actualizar(any(), any());
	}

	@Test
	void noDejaDesactivarAlUnicoAdministrador() {
		assertThatThrownBy(() -> service.actualizar(ID, request("admin.junta", false, RolCodigoEnum.ADMIN)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("único administrador");

		assertThat(usuario.isActivo()).isTrue();
		verify(refreshTokenRepository, never()).revocarTodosDeUsuario(anyLong());
	}

	@Test
	void noDejaQuitarElRolAlUnicoAdministrador() {
		assertThatThrownBy(() -> service.actualizar(ID, request("admin.junta", true, RolCodigoEnum.PRIOSTE)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("único administrador");

		assertThat(usuario.getRoles()).containsExactly(rolAdmin);
	}

	@Test
	void dejaDesactivarUnAdministradorSiQuedaOtro() {
		when(usuarioRepository.existsByActivoTrueAndRoles_CodigoAndIdNot(RolCodigoEnum.ADMIN, ID)).thenReturn(true);

		service.actualizar(ID, request("admin.junta", false, RolCodigoEnum.ADMIN));

		assertThat(usuario.isActivo()).isFalse();
		verify(refreshTokenRepository).revocarTodosDeUsuario(ID);
	}

	@Test
	void cambiarLosRolesCierraSusSesiones() {
		when(usuarioRepository.existsByActivoTrueAndRoles_CodigoAndIdNot(RolCodigoEnum.ADMIN, ID)).thenReturn(true);

		service.actualizar(ID, request("admin.junta", true, RolCodigoEnum.PRIOSTE));

		assertThat(usuario.getRoles()).containsExactly(rolPrioste);
		verify(refreshTokenRepository).revocarTodosDeUsuario(ID);
	}

	@Test
	void cambiarElNombreDeUsuarioCierraSusSesiones() {
		service.actualizar(ID, request("administrador", true, RolCodigoEnum.ADMIN));

		verify(refreshTokenRepository).revocarTodosDeUsuario(ID);
	}

	@Test
	void editarOtrosDatosNoCierraSusSesiones() {
		service.actualizar(ID, request("admin.junta", true, RolCodigoEnum.ADMIN));

		verify(refreshTokenRepository, never()).revocarTodosDeUsuario(anyLong());
	}

	@Test
	void noDejaUsarUnNombreDeUsuarioRepetido() {
		when(usuarioRepository.existsByUsernameAndIdNot("otro", ID)).thenReturn(true);

		assertThatThrownBy(() -> service.actualizar(ID, request("otro", true, RolCodigoEnum.ADMIN)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("Ya existe");
	}

	@Test
	void alCrearGeneraUnaContrasenaTemporalQueHayQueCambiar() {
		UsuarioEntity nuevo = new UsuarioEntity();
		when(usuarioMapper.toEntity(any())).thenReturn(nuevo);
		when(usuarioRepository.save(nuevo)).thenReturn(nuevo);

		UsuarioCreadoDTO creado = service.crear(request("nuevo", true, RolCodigoEnum.PRIOSTE));

		assertThat(creado.passwordTemporal()).hasSize(12);
		assertThat(nuevo.getPasswordHash()).isNotEqualTo(creado.passwordTemporal());
		assertThat(passwordEncoder.matches(creado.passwordTemporal(), nuevo.getPasswordHash())).isTrue();
		assertThat(nuevo.isDebeCambiarPassword()).isTrue();
		assertThat(nuevo.getRoles()).containsExactly(rolPrioste);
	}

	@Test
	void restablecerDesbloqueaYCierraSusSesiones() {
		usuario.setIntentosFallidos(5);
		usuario.setBloqueadoHasta(java.time.Instant.now().plusSeconds(600));
		usuario.setDebeCambiarPassword(false);

		UsuarioCreadoDTO resultado = service.restablecerPassword(ID);

		assertThat(passwordEncoder.matches(resultado.passwordTemporal(), usuario.getPasswordHash())).isTrue();
		assertThat(usuario.isDebeCambiarPassword()).isTrue();
		assertThat(usuario.getIntentosFallidos()).isZero();
		assertThat(usuario.getBloqueadoHasta()).isNull();
		verify(refreshTokenRepository).revocarTodosDeUsuario(ID);
	}

	private static UsuarioRequest request(String username, boolean activo, RolCodigoEnum... roles) {
		return new UsuarioRequest(username, "Nombre", null, CargoUsuarioEnum.ADMINISTRADOR, null, activo, Set.of(roles));
	}

	private static RolEntity rol(RolCodigoEnum codigo) {
		RolEntity rol = new RolEntity();
		rol.setCodigo(codigo);
		return rol;
	}

}
