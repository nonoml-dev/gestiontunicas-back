package es.nonomldev.gestiontunicas.hermano.service.impl;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDto;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDto;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.hermano.mapper.HermanoMapper;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.hermano.service.HermanoService;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HermanoServiceImpl implements HermanoService {

	/** Edad máxima por defecto si no hay reparto abierto (10–15 años). */
	private static final int EDAD_MAXIMA_POR_DEFECTO = 15;

	private final HermanoRepository hermanoRepository;
	private final RepartoRepository repartoRepository;
	private final HermanoMapper hermanoMapper;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Page<HermanoResumenDto> buscar(String texto, Pageable pageable) {
		String normalizado = texto == null ? "" : Hermano.normalizar(texto);
		return hermanoRepository.buscar(normalizado, limiteFechaNacimiento(), pageable)
				.map(hermanoMapper::toResumenDto);
	}

	@Override
	@Transactional(readOnly = true)
	public HermanoDto obtener(Long id) {
		return hermanoMapper.toDto(buscarEntidad(id));
	}

	@Override
	public HermanoDto crear(HermanoRequest request) {
		Hermano hermano = hermanoMapper.toEntity(request);
		hermano.setActivo(true);
		actualizarConsentimiento(hermano, request.consentimientoRgpd());
		return hermanoMapper.toDto(hermanoRepository.save(hermano));
	}

	@Override
	public HermanoDto actualizar(Long id, HermanoRequest request) {
		Hermano hermano = buscarEntidad(id);
		boolean teniaConsentimiento = hermano.isConsentimientoRgpd();
		hermanoMapper.actualizar(request, hermano);
		if (teniaConsentimiento != request.consentimientoRgpd()) {
			actualizarConsentimiento(hermano, request.consentimientoRgpd());
		}
		return hermanoMapper.toDto(hermano);
	}

	@Override
	public void desactivar(Long id) {
		buscarEntidad(id).setActivo(false);
	}

	private Hermano buscarEntidad(Long id) {
		return hermanoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el hermano", id));
	}

	private void actualizarConsentimiento(Hermano hermano, boolean consiente) {
		hermano.setFechaConsentimiento(consiente ? LocalDate.now(clock) : null);
	}

	/**
	 * Fecha de nacimiento a partir de la cual el hermano aún no ha superado la edad máxima a Jueves Santo:
	 * los nacidos en esa fecha o antes ya han cumplido (edadMax + 1) años y no salen en el buscador.
	 */
	private LocalDate limiteFechaNacimiento() {
		return repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO)
				.map(r -> r.getFechaJuevesSanto().minusYears(r.getEdadMax() + 1L))
				.orElseGet(() -> proximoJuevesSanto().minusYears(EDAD_MAXIMA_POR_DEFECTO + 1L));
	}

	private LocalDate proximoJuevesSanto() {
		LocalDate hoy = LocalDate.now(clock);
		LocalDate esteAnio = Reparto.calcularJuevesSanto(hoy.getYear());
		return esteAnio.isBefore(hoy) ? Reparto.calcularJuevesSanto(hoy.getYear() + 1) : esteAnio;
	}

}
