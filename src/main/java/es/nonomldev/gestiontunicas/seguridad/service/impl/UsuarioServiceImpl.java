package es.nonomldev.gestiontunicas.seguridad.service.impl;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDto;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDto;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.Rol;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import es.nonomldev.gestiontunicas.seguridad.mapper.UsuarioMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.RefreshTokenRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

	private static final String CARACTERES_PASSWORD = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
	private static final int LONGITUD_PASSWORD_TEMPORAL = 12;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final UsuarioRepository usuarioRepository;
	private final RolRepository rolRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UsuarioMapper usuarioMapper;
	private final PasswordEncoder passwordEncoder;
	private final UsuarioActualService usuarioActualService;

	@Override
	@Transactional(readOnly = true)
	public List<UsuarioDto> listar() {
		return usuarioRepository.findAll().stream().map(usuarioMapper::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public UsuarioDto obtener(Long id) {
		return usuarioMapper.toDto(buscar(id));
	}

	@Override
	public UsuarioCreadoDto crear(UsuarioRequest request) {
		if (usuarioRepository.existsByUsername(request.username())) {
			throw new ReglaNegocioException("Ya existe un usuario con el nombre " + request.username());
		}
		Usuario usuario = usuarioMapper.toEntity(request);
		usuario.setRoles(roles(request.roles()));
		String temporal = generarPasswordTemporal();
		usuario.setPasswordHash(passwordEncoder.encode(temporal));
		usuario.setDebeCambiarPassword(true);
		return new UsuarioCreadoDto(usuarioMapper.toDto(usuarioRepository.save(usuario)), temporal);
	}

	@Override
	public UsuarioDto actualizar(Long id, UsuarioRequest request) {
		Usuario usuario = buscar(id);
		if (usuarioRepository.existsByUsernameAndIdNot(request.username(), id)) {
			throw new ReglaNegocioException("Ya existe un usuario con el nombre " + request.username());
		}
		boolean seDesactiva = usuario.isActivo() && !request.activo();
		usuarioMapper.actualizar(request, usuario);
		usuario.setRoles(roles(request.roles()));
		if (seDesactiva) {
			refreshTokenRepository.revocarTodosDeUsuario(id);
		}
		return usuarioMapper.toDto(usuario);
	}

	@Override
	public UsuarioCreadoDto restablecerPassword(Long id) {
		Usuario usuario = buscar(id);
		String temporal = generarPasswordTemporal();
		usuario.setPasswordHash(passwordEncoder.encode(temporal));
		usuario.setDebeCambiarPassword(true);
		usuario.setIntentosFallidos(0);
		usuario.setBloqueadoHasta(null);
		refreshTokenRepository.revocarTodosDeUsuario(id);
		return new UsuarioCreadoDto(usuarioMapper.toDto(usuario), temporal);
	}

	@Override
	public void cambiarPasswordPropia(CambioPasswordRequest request) {
		Usuario usuario = usuarioActualService.obtener();
		if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
			throw new ReglaNegocioException("La contraseña actual no es correcta");
		}
		if (request.passwordActual().equals(request.passwordNueva())) {
			throw new ReglaNegocioException("La contraseña nueva tiene que ser distinta de la actual");
		}
		usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
		usuario.setDebeCambiarPassword(false);
	}

	private Usuario buscar(Long id) {
		return usuarioRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el usuario", id));
	}

	private Set<Rol> roles(Set<RolCodigo> codigos) {
		List<Rol> roles = rolRepository.findByCodigoIn(codigos);
		if (roles.size() != codigos.size()) {
			throw new ReglaNegocioException("Alguno de los roles indicados no existe");
		}
		return new HashSet<>(roles);
	}

	private static String generarPasswordTemporal() {
		StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD_TEMPORAL);
		for (int i = 0; i < LONGITUD_PASSWORD_TEMPORAL; i++) {
			sb.append(CARACTERES_PASSWORD.charAt(RANDOM.nextInt(CARACTERES_PASSWORD.length())));
		}
		return sb.toString();
	}

}
