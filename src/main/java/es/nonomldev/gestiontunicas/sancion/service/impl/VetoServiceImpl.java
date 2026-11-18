package es.nonomldev.gestiontunicas.sancion.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.sancion.dto.VetoDTO;
import es.nonomldev.gestiontunicas.sancion.dto.VetoRequest;
import es.nonomldev.gestiontunicas.sancion.entity.VetoEntity;
import es.nonomldev.gestiontunicas.sancion.mapper.SancionMapper;
import es.nonomldev.gestiontunicas.sancion.repository.VetoRepository;
import es.nonomldev.gestiontunicas.sancion.service.VetoService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class VetoServiceImpl implements VetoService {

	private final VetoRepository vetoRepository;
	private final HermanoRepository hermanoRepository;
	private final SancionMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<VetoDTO> deHermano(Long hermanoId) {
		return mapper.toVetoDTOs(vetoRepository.findByHermanoIdOrderByFechaDesc(hermanoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<VetoDTO> vigentes(Integer anio) {
		return mapper.toVetoDTOs(vetoRepository.findByAnioVetadoAndActivoTrueOrderByFechaDesc(anio));
	}

	@Override
	public VetoDTO imponer(VetoRequest request) {
		if (vetoRepository.existsByHermanoIdAndAnioVetadoAndActivoTrue(request.hermanoId(), request.anioVetado())) {
			throw new ReglaNegocioException("El hermano ya está vetado para el reparto " + request.anioVetado());
		}
		VetoEntity veto = mapper.toEntity(request);
		veto.setHermano(hermanoRepository.findById(request.hermanoId())
				.orElseThrow(() -> new RecursoNoEncontradoException("el hermano", request.hermanoId())));
		veto.setFecha(Instant.now(clock));
		veto.setUsuario(usuarioActualService.obtener());
		return mapper.toDTO(vetoRepository.save(veto));
	}

	@Override
	public VetoDTO levantar(Long id, MotivoRequest request) {
		VetoEntity veto = vetoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el veto", id));
		if (!veto.isActivo()) {
			throw new ReglaNegocioException("El veto ya está levantado");
		}
		veto.levantar(usuarioActualService.obtener(), request.motivo());
		veto.setFechaLevantamiento(Instant.now(clock));
		return mapper.toDTO(veto);
	}

}
