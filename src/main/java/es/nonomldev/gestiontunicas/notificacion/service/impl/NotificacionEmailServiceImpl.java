package es.nonomldev.gestiontunicas.notificacion.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.notificacion.dto.NotificacionEmailDto;
import es.nonomldev.gestiontunicas.notificacion.mapper.NotificacionEmailMapper;
import es.nonomldev.gestiontunicas.notificacion.repository.NotificacionEmailRepository;
import es.nonomldev.gestiontunicas.notificacion.service.NotificacionEmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacionEmailServiceImpl implements NotificacionEmailService {

	private final NotificacionEmailRepository repository;
	private final NotificacionEmailMapper mapper;

	@Override
	public List<NotificacionEmailDto> deHermano(Long hermanoId) {
		return mapper.toDtos(repository.findByHermanoIdOrderByCreadoEnDesc(hermanoId));
	}

}
