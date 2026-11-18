package es.nonomldev.gestiontunicas.hermano.service.impl;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.hermano.dto.ContactoHermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.CriterioEdadDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.hermano.mapper.HermanoMapper;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.hermano.service.HermanoService;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HermanoServiceImpl implements HermanoService {

	/** Rango de edad por defecto si no hay reparto abierto (10–15 años a Jueves Santo). */
	private static final int EDAD_MINIMA_POR_DEFECTO = 10;
	private static final int EDAD_MAXIMA_POR_DEFECTO = 15;

	private final HermanoRepository hermanoRepository;
	private final RepartoRepository repartoRepository;
	private final HermanoMapper hermanoMapper;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Page<HermanoResumenDTO> buscar(String texto, boolean soloEnEdad, Pageable pageable) {
		String normalizado = texto == null ? "" : HermanoEntity.normalizar(texto);
		if (!soloEnEdad) {
			return hermanoRepository.buscar(normalizado, pageable).map(hermanoMapper::toResumenDTO);
		}
		// Edad >= mínima  ⇔ nacido en (Jueves Santo − mínima años) o antes.
		// Edad <= máxima  ⇔ nacido después de (Jueves Santo − (máxima + 1) años).
		CriterioEdadDTO criterio = criterioEdad();
		LocalDate nacidoHasta = criterio.juevesSanto().minusYears(criterio.edadMin());
		LocalDate nacidoDespuesDe = criterio.juevesSanto().minusYears(criterio.edadMax() + 1L);
		return hermanoRepository.buscarEnEdad(normalizado, nacidoDespuesDe, nacidoHasta, pageable)
				.map(hermanoMapper::toResumenDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public CriterioEdadDTO criterioEdad() {
		return repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)
				.map(r -> new CriterioEdadDTO(r.getFechaJuevesSanto(), r.getEdadMin(), r.getEdadMax(), r.getAnio()))
				.orElseGet(() -> new CriterioEdadDTO(proximoJuevesSanto(), EDAD_MINIMA_POR_DEFECTO,
						EDAD_MAXIMA_POR_DEFECTO, null));
	}

	@Override
	@Transactional(readOnly = true)
	public HermanoDTO obtener(Long id) {
		return hermanoMapper.toDTO(buscarEntidad(id));
	}

	@Override
	public HermanoDTO crear(HermanoRequest request) {
		HermanoEntity hermano = hermanoMapper.toEntity(request);
		hermano.setActivo(true);
		return hermanoMapper.toDTO(hermanoRepository.save(hermano));
	}

	@Override
	public HermanoDTO actualizarContacto(Long id, ContactoHermanoRequest request) {
		HermanoEntity hermano = buscarEntidad(id);
		hermano.setTelefono1(sinEspacios(request.telefono1()));
		hermano.setTelefono2(sinEspacios(request.telefono2()));
		hermano.setEmail(sinEspacios(request.email()));
		return hermanoMapper.toDTO(hermano);
	}

	/** Texto sin espacios a los lados; vacío = nulo. */
	private static String sinEspacios(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}

	@Override
	public void desactivar(Long id) {
		buscarEntidad(id).setActivo(false);
	}

	private HermanoEntity buscarEntidad(Long id) {
		return hermanoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el hermano", id));
	}

	private LocalDate proximoJuevesSanto() {
		LocalDate hoy = LocalDate.now(clock);
		LocalDate esteAnio = RepartoEntity.calcularJuevesSanto(hoy.getYear());
		return esteAnio.isBefore(hoy) ? RepartoEntity.calcularJuevesSanto(hoy.getYear() + 1) : esteAnio;
	}

}
