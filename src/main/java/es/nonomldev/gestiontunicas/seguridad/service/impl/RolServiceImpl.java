package es.nonomldev.gestiontunicas.seguridad.service.impl;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.RolPermisosRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.PermisoEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;
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
	public List<RolDTO> listar() {
		return rolMapper.toDTOs(rolRepository.findAllByOrderByCodigo());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PermisoDTO> listarPermisos() {
		return rolMapper.toPermisoDTOs(permisoRepository.findAllByOrderByModuloAscCodigoAsc());
	}

	@Override
	public RolDTO actualizarPermisos(Long rolId, RolPermisosRequest request) {
		RolEntity rol = rolRepository.findById(rolId).orElseThrow(() -> new RecursoNoEncontradoException("el rol", rolId));
		List<PermisoEntity> permisos = permisoRepository.findByCodigoIn(request.permisos());
		if (permisos.size() != request.permisos().size()) {
			throw new ReglaNegocioException("Alguno de los permisos indicados no existe");
		}
		rol.setPermisos(new HashSet<>(permisos));
		return rolMapper.toDTO(rol);
	}

}
