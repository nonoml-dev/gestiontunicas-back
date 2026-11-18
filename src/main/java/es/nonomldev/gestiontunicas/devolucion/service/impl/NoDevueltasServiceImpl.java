package es.nonomldev.gestiontunicas.devolucion.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDto;
import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.mapper.AlquilerMapper;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.comun.util.Formato;
import es.nonomldev.gestiontunicas.devolucion.service.NoDevueltasService;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucion;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.service.GeneradorSanciones;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NoDevueltasServiceImpl implements NoDevueltasService {

	private final RepartoRepository repartoRepository;
	private final AlquilerRepository alquilerRepository;
	private final GeneradorSanciones generador;
	private final AlquilerMapper alquilerMapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDto> pendientesFinPeriodo(Long repartoId) {
		return alquilerMapper.toDtos(alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquiler.ACTIVO));
	}

	@Override
	public int procesarFinPeriodo(Long repartoId) {
		Reparto reparto = buscar(repartoId);
		PeriodoDevolucion periodo = reparto.getPeriodoDevolucion();
		if (reparto.getNoDevueltasProcesadasEn() != null) {
			throw new ReglaNegocioException("Las no devueltas del reparto %d ya se procesaron".formatted(reparto.getAnio()));
		}
		if (!LocalDate.now(clock).isAfter(periodo.getFin())) {
			throw new ReglaNegocioException("El periodo de devolución termina el " + periodo.getFin().format(Formato.FECHA));
		}
		Usuario usuario = usuarioActualService.obtener();
		List<Alquiler> pendientes = alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquiler.ACTIVO);
		for (Alquiler alquiler : pendientes) {
			alquiler.setEstado(EstadoAlquiler.NO_DEVUELTO);
			alquiler.getTunica().setEstado(EstadoTunica.NO_DEVUELTA);
			generador.sancionar(alquiler, null, ConceptoSancion.RETRASO);
			generador.vetarPorNoDevolverEnPlazo(alquiler, usuario);
		}
		reparto.setNoDevueltasProcesadasEn(Instant.now(clock));
		return pendientes.size();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDto> pendientesFinPlazoGracia(Long repartoId) {
		return alquilerMapper.toDtos(alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquiler.NO_DEVUELTO));
	}

	@Override
	public int procesarFinPlazoGracia(Long repartoId) {
		Reparto reparto = buscar(repartoId);
		PeriodoDevolucion periodo = reparto.getPeriodoDevolucion();
		if (reparto.getNoDevueltasProcesadasEn() == null) {
			throw new ReglaNegocioException("Primero hay que procesar las no devueltas al terminar el periodo de devolución");
		}
		if (reparto.getFinGraciaProcesadoEn() != null) {
			throw new ReglaNegocioException("El plazo de gracia del reparto %d ya se cerró".formatted(reparto.getAnio()));
		}
		if (!LocalDate.now(clock).isAfter(periodo.finPlazoGracia())) {
			throw new ReglaNegocioException("El plazo de gracia termina el " + periodo.finPlazoGracia().format(Formato.FECHA));
		}
		LocalDate hoy = LocalDate.now(clock);
		List<Alquiler> pendientes = alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquiler.NO_DEVUELTO);
		for (Alquiler alquiler : pendientes) {
			alquiler.setEstado(EstadoAlquiler.ADQUIRIDA_POR_HERMANO);
			Tunica tunica = alquiler.getTunica();
			tunica.setEstado(EstadoTunica.PROPIEDAD_HERMANO); // sale del inventario y libera su código
			tunica.setFechaPropiedad(hoy);
			generador.sancionar(alquiler, null, ConceptoSancion.RETRASO); // por si no se generó
			generador.sancionar(alquiler, null, ConceptoSancion.CARGO_TOTAL);
		}
		reparto.setFinGraciaProcesadoEn(Instant.now(clock));
		return pendientes.size();
	}

	private Reparto buscar(Long id) {
		return repartoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el reparto", id));
	}

}
