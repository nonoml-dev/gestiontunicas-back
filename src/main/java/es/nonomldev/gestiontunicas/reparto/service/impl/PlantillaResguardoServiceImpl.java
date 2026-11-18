package es.nonomldev.gestiontunicas.reparto.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDto;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardo;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapper;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.service.PlantillaResguardoService;
import es.nonomldev.gestiontunicas.reparto.service.VariablesResguardo;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PlantillaResguardoServiceImpl implements PlantillaResguardoService {

	private final PlantillaResguardoRepository repository;
	private final RepartoMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	public PlantillaResguardoDto subir(String nombre, String nombreFichero, byte[] contenido) {
		if (nombreFichero == null || !nombreFichero.toLowerCase().endsWith(".docx")) {
			throw new ReglaNegocioException("La plantilla tiene que ser un documento Word (.docx)");
		}
		if (contenido == null || contenido.length == 0) {
			throw new ReglaNegocioException("El fichero de la plantilla está vacío");
		}
		PlantillaResguardo plantilla = new PlantillaResguardo();
		plantilla.setNombre(nombre == null || nombre.isBlank() ? nombreFichero : nombre);
		plantilla.setNombreFichero(nombreFichero);
		plantilla.setContenido(contenido);
		plantilla.setVariablesDetectadas(new ArrayList<>(detectarVariables(contenido)));
		plantilla.setSubidoPor(usuarioActualService.obtener());
		plantilla.setSubidoEn(Instant.now(clock));
		return mapper.toDto(repository.save(plantilla));
	}

	@Override
	@Transactional(readOnly = true)
	public List<PlantillaResguardoDto> listar() {
		return mapper.toPlantillaDtos(repository.findAllByOrderBySubidoEnDesc());
	}

	@Override
	@Transactional(readOnly = true)
	public PlantillaResguardoDto obtener(Long id) {
		return mapper.toDto(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public byte[] contenido(Long id) {
		return buscar(id).getContenido();
	}

	/** Lee el texto completo del Word (párrafos, tablas, cabeceras y pies) y busca las {{variables}}. */
	private static List<String> detectarVariables(byte[] contenido) {
		try (XWPFDocument documento = new XWPFDocument(new ByteArrayInputStream(contenido));
				XWPFWordExtractor extractor = new XWPFWordExtractor(documento)) {
			return VariablesResguardo.detectar(extractor.getText());
		} catch (IOException | RuntimeException e) {
			throw new ReglaNegocioException("No se puede leer el documento Word: " + e.getMessage());
		}
	}

	private PlantillaResguardo buscar(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("la plantilla del resguardo", id));
	}

}
