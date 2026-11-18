package es.nonomldev.gestiontunicas.devolucion.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionDto;
import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionRequest;
import es.nonomldev.gestiontunicas.devolucion.mapper.DevolucionMapper;
import es.nonomldev.gestiontunicas.devolucion.repository.DevolucionRepository;
import es.nonomldev.gestiontunicas.devolucion.service.DevolucionService;
import es.nonomldev.gestiontunicas.devolucion.service.ProcesadorDevolucion;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DevolucionServiceImpl implements DevolucionService {

	private final AlquilerRepository alquilerRepository;
	private final TunicaRepository tunicaRepository;
	private final DevolucionRepository devolucionRepository;
	private final ProcesadorDevolucion procesador;
	private final DevolucionMapper mapper;
	private final UsuarioActualService usuarioActualService;

	@Override
	public DevolucionDto registrar(DevolucionRequest request) {
		Alquiler alquiler = alquilerRepository.findById(request.alquilerId())
				.orElseThrow(() -> new RecursoNoEncontradoException("la entrega", request.alquilerId()));
		// Bloquea la túnica mientras se registra (dos puestos a la vez)
		tunicaRepository.findByIdParaActualizar(alquiler.getTunica().getId());
		return mapper.toDto(procesador.procesar(alquiler, request.revisiones(), request.observaciones(), false,
				usuarioActualService.obtener()));
	}

	@Override
	@Transactional(readOnly = true)
	public DevolucionDto obtenerPorAlquiler(Long alquilerId) {
		return mapper.toDto(devolucionRepository.findByAlquilerId(alquilerId)
				.orElseThrow(() -> new RecursoNoEncontradoException("La entrega %d no tiene devolución".formatted(alquilerId))));
	}

}
