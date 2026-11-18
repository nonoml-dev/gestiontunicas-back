package es.nonomldev.gestiontunicas.tunica.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDto;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDto;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import es.nonomldev.gestiontunicas.tunica.mapper.TunicaMapper;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import es.nonomldev.gestiontunicas.tunica.service.TunicaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TunicaServiceImpl implements TunicaService {

	/** Tolerancia si no hay reparto abierto (±3 cm, como el valor por defecto). */
	private static final int TOLERANCIA_POR_DEFECTO = 3;

	/** Estados en los que la túnica está fuera de casa: no se puede dar de baja. */
	private static final Set<EstadoTunica> EN_USO = EnumSet.of(EstadoTunica.RESERVADA, EstadoTunica.ALQUILADA,
			EstadoTunica.NO_DEVUELTA, EstadoTunica.PROPIEDAD_HERMANO);

	private final TunicaRepository tunicaRepository;
	private final RepartoRepository repartoRepository;
	private final TunicaMapper mapper;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Page<TunicaResumenDto> buscar(Set<EstadoTunica> estados, Integer medidaMin, Integer medidaMax,
			Pageable pageable) {
		Set<EstadoTunica> filtro = estados == null || estados.isEmpty()
				? EnumSet.complementOf(EnumSet.of(EstadoTunica.PROPIEDAD_HERMANO))
				: estados;
		return tunicaRepository.buscar(filtro, medidaMin, medidaMax, pageable).map(mapper::toResumenDto);
	}

	@Override
	@Transactional(readOnly = true)
	public TunicaDto obtener(Long id) {
		return mapper.toDto(tunicaRepository.findWithPiezasById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("la túnica", id)));
	}

	@Override
	public TunicaDto crear(TunicaRequest request) {
		comprobarCodigoLibre(request.codigo());
		Tunica tunica = mapper.toEntity(request);
		tunica.setEstado(EstadoTunica.DISPONIBLE);
		if (tunica.getFechaAlta() == null) {
			tunica.setFechaAlta(LocalDate.now(clock));
		}
		tunica.crearPiezas();
		return mapper.toDto(tunicaRepository.save(tunica));
	}

	@Override
	public TunicaDto actualizar(Long id, TunicaRequest request) {
		Tunica tunica = buscarEntidad(id);
		if (!tunica.enInventario()) {
			throw new ReglaNegocioException("La túnica %s es propiedad de un hermano y no se puede modificar"
					.formatted(tunica.getCodigo()));
		}
		if (!tunica.getCodigo().equals(request.codigo())) {
			comprobarCodigoLibre(request.codigo());
		}
		mapper.actualizar(request, tunica);
		tunicaRepository.flush(); // refresca la medida total, que calcula la BD
		return mapper.toDto(tunica);
	}

	@Override
	public TunicaDto darDeBaja(Long id, MotivoRequest request) {
		Tunica tunica = buscarEntidad(id);
		if (EN_USO.contains(tunica.getEstado())) {
			throw new ReglaNegocioException("La túnica %s no se puede dar de baja: está %s"
					.formatted(tunica.getCodigo(), tunica.getEstado().name().toLowerCase().replace('_', ' ')));
		}
		tunica.setEstado(EstadoTunica.BAJA);
		tunica.setFechaBaja(LocalDate.now(clock));
		tunica.setMotivoBaja(request.motivo());
		return mapper.toDto(tunica);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean codigoFueDeHermano(String codigo) {
		return tunicaRepository.existsByCodigoAndEstado(codigo, EstadoTunica.PROPIEDAD_HERMANO);
	}

	@Override
	@Transactional(readOnly = true)
	public List<TunicaResumenDto> proponer(int alturaCm, Integer toleranciaCm) {
		int tolerancia = toleranciaCm != null ? toleranciaCm
				: repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO)
						.map(Reparto::getToleranciaMedidaCm)
						.orElse(TOLERANCIA_POR_DEFECTO);
		return mapper.toResumenDtos(tunicaRepository.proponerPorAltura(alturaCm, tolerancia));
	}

	private void comprobarCodigoLibre(String codigo) {
		if (tunicaRepository.existeEnInventario(codigo)) {
			throw new ReglaNegocioException("Ya hay una túnica en el inventario con el código " + codigo);
		}
	}

	private Tunica buscarEntidad(Long id) {
		return tunicaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("la túnica", id));
	}

}
