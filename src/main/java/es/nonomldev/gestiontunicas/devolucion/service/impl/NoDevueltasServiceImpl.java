package es.nonomldev.gestiontunicas.devolucion.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDTO;
import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.mapper.AlquilerMapper;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.comun.util.Formato;
import es.nonomldev.gestiontunicas.devolucion.service.NoDevueltasService;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.service.GeneradorSanciones;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
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
	public List<AlquilerDTO> pendientesFinPeriodo(Long repartoId) {
		return alquilerMapper.toDTOs(alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquilerEnum.ACTIVO));
	}

	@Override
	public int procesarFinPeriodo(Long repartoId) {
		RepartoEntity reparto = buscar(repartoId);
		PeriodoDevolucionEntity periodo = reparto.getPeriodoDevolucion();
		if (reparto.getNoDevueltasProcesadasEn() != null) {
			throw new ReglaNegocioException("Las no devueltas del reparto %d ya se procesaron".formatted(reparto.getAnio()));
		}
		if (!LocalDate.now(clock).isAfter(periodo.getFin())) {
			throw new ReglaNegocioException("El periodo de devolución termina el " + periodo.getFin().format(Formato.FECHA));
		}
		UsuarioEntity usuario = usuarioActualService.obtener();
		List<AlquilerEntity> pendientes = alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquilerEnum.ACTIVO);
		for (AlquilerEntity alquiler : pendientes) {
			alquiler.setEstado(EstadoAlquilerEnum.NO_DEVUELTO);
			alquiler.getTunica().setEstado(EstadoTunicaEnum.NO_DEVUELTA);
			generador.sancionar(alquiler, null, ConceptoSancionEnum.RETRASO);
			generador.vetarPorNoDevolverEnPlazo(alquiler, usuario);
		}
		reparto.setNoDevueltasProcesadasEn(Instant.now(clock));
		return pendientes.size();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDTO> pendientesFinPlazoGracia(Long repartoId) {
		return alquilerMapper.toDTOs(alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquilerEnum.NO_DEVUELTO));
	}

	@Override
	public int procesarFinPlazoGracia(Long repartoId) {
		RepartoEntity reparto = buscar(repartoId);
		PeriodoDevolucionEntity periodo = reparto.getPeriodoDevolucion();
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
		List<AlquilerEntity> pendientes = alquilerRepository.findByRepartoIdAndEstado(repartoId, EstadoAlquilerEnum.NO_DEVUELTO);
		for (AlquilerEntity alquiler : pendientes) {
			alquiler.setEstado(EstadoAlquilerEnum.ADQUIRIDA_POR_HERMANO);
			TunicaEntity tunica = alquiler.getTunica();
			tunica.setEstado(EstadoTunicaEnum.PROPIEDAD_HERMANO); // sale del inventario y libera su código
			tunica.setFechaPropiedad(hoy);
			generador.sancionar(alquiler, null, ConceptoSancionEnum.RETRASO); // por si no se generó
			generador.sancionar(alquiler, null, ConceptoSancionEnum.CARGO_TOTAL);
		}
		reparto.setFinGraciaProcesadoEn(Instant.now(clock));
		return pendientes.size();
	}

	private RepartoEntity buscar(Long id) {
		return repartoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el reparto", id));
	}

}
