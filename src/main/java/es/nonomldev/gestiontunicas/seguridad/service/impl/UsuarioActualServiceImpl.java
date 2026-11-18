package es.nonomldev.gestiontunicas.seguridad.service.impl;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioActualServiceImpl implements UsuarioActualService {

	private final UsuarioRepository usuarioRepository;

	@Override
	public UsuarioEntity obtener() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
			throw new ReglaNegocioException("No hay ningún usuario autenticado");
		}
		return usuarioRepository.findByUsername(auth.getName())
				.orElseThrow(() -> new ReglaNegocioException("El usuario autenticado no existe: " + auth.getName()));
	}

}
