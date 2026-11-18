package es.nonomldev.gestiontunicas.seguridad.service.impl;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDto;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDto;
import es.nonomldev.gestiontunicas.seguridad.dto.RolPermisosRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.Permiso;
import es.nonomldev.gestiontunicas.seguridad.entity.Rol;
import es.nonomldev.gestiontunicas.seguridad.mapper.RolMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.PermisoRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.RolRepository;
import es.nonomldev.gestiontunicas.seguridad.service.RolService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RolServiceImpl implements RolService {

	private final RolRepository rolRepository;
	private final PermisoRepository permisoRepository;
	private final RolMapper rolMapper;

	@Override
	@Transactional(readOnly = true)
	public List<RolDto> listar() {
		return rolMapper.toDtos(rolRepository.findAllByOrderByCodigo());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermisoDto> listarPermisos() {
		return rolMapper.toPermisoDtos(permisoRepository.findAllByOrderByModuloAscCodigoAsc());
	}

	@Override
	public RolDto actualizarPermisos(Long rolId, RolPermisosRequest request) {
		Rol rol = rolRepository.findById(rolId).orElseThrow(() -> new RecursoNoEncontradoException("el rol", rolId));
		List<Permiso> permisos = permisoRepository.findByCodigoIn(request.permisos());
		if (permisos.size() != request.permisos().size()) {
			throw new ReglaNegocioException("Alguno de los permisos indicados no existe");
		}
		rol.setPermisos(new HashSet<>(permisos));
		return rolMapper.toDto(rol);
	}

}
