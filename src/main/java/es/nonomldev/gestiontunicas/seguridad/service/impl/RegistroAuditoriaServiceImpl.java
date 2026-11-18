package es.nonomldev.gestiontunicas.seguridad.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.seguridad.dto.RegistroAuditoriaDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.RegistroAuditoriaEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;
import es.nonomldev.gestiontunicas.seguridad.mapper.RegistroAuditoriaMapper;
import es.nonomldev.gestiontunicas.seguridad.repository.RegistroAuditoriaRepository;
import es.nonomldev.gestiontunicas.seguridad.repository.UsuarioRepository;
import es.nonomldev.gestiontunicas.seguridad.service.RegistroAuditoriaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegistroAuditoriaServiceImpl implements RegistroAuditoriaService {

	private static final int MAX_USERNAME = 50;
	private static final Instant SIN_LIMITE_INICIO = Instant.parse("2000-01-01T00:00:00Z");
	private static final Instant SIN_LIMITE_FIN = Instant.parse("9999-01-01T00:00:00Z");

	private final RegistroAuditoriaRepository registroRepository;
	private final UsuarioRepository usuarioRepository;
	private final RegistroAuditoriaMapper mapper;
	private final Clock clock;

	/** En su propia transacción: el intento queda registrado aunque falle lo demás. */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrar(String username, ResultadoAccesoEnum resultado, String ip) {
		String nombre = username == null ? "" : username.trim();
		RegistroAuditoriaEntity registro = new RegistroAuditoriaEntity();
		registro.setFecha(Instant.now(clock));
		registro.setUsername(nombre.length() > MAX_USERNAME ? nombre.substring(0, MAX_USERNAME) : nombre);
		registro.setResultado(resultado);
		registro.setIp(ip);
		usuarioRepository.findByUsername(nombre).ifPresent(registro::setUsuario);
		registroRepository.save(registro);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<RegistroAuditoriaDTO> buscar(String username, ResultadoAccesoEnum resultado, LocalDate desde,
			LocalDate hasta, Pageable pageable) {
		// Sin filtro de fechas se usan límites amplios (la consulta no admite nulos en las fechas).
		Instant inicio = desde == null ? SIN_LIMITE_INICIO : desde.atStartOfDay(clock.getZone()).toInstant();
		Instant fin = hasta == null ? SIN_LIMITE_FIN : hasta.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
		String filtro = username == null || username.isBlank() ? null : username.trim();
		return registroRepository.buscar(filtro, resultado, inicio, fin, pageable).map(mapper::toDTO);
	}

}
