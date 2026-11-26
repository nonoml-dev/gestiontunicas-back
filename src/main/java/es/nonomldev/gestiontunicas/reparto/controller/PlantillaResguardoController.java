package es.nonomldev.gestiontunicas.reparto.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDTO;
import es.nonomldev.gestiontunicas.reparto.service.PlantillaResguardoService;
import lombok.RequiredArgsConstructor;

/**
 * Plantillas Word (.docx) del resguardo. Solo las suben y consultan Administrador y Prioste (matriz del
 * análisis §3). Una plantilla no se edita: para cambiarla se sube otra y se asigna al reparto.
 */
@RestController
@RequestMapping("/api/v1/plantillas-resguardo")
@RequiredArgsConstructor
@PreAuthorize(RepartoController.PUEDE_GESTIONAR)
public class PlantillaResguardoController {

	private static final MediaType DOCX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

	private final PlantillaResguardoService plantillaService;

	/**
	 * Sube el Word y devuelve las variables encontradas. Las que no están en el catálogo salen en
	 * {@code variablesDesconocidas}, para corregir una errata antes de usarla. Sin {@code nombre}, se usa
	 * el del fichero.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PlantillaResguardoDTO subir(@RequestParam("fichero") MultipartFile fichero,
			@RequestParam(required = false) String nombre) throws IOException {
		return plantillaService.subir(nombre, fichero.getOriginalFilename(), fichero.getBytes());
	}

	/** De la más reciente a la más antigua. */
	@GetMapping
	public List<PlantillaResguardoDTO> listar() {
		return plantillaService.listar();
	}

	@GetMapping("/{id}")
	public PlantillaResguardoDTO obtener(@PathVariable Long id) {
		return plantillaService.obtener(id);
	}

	/** El .docx tal como se subió. */
	@GetMapping("/{id}/fichero")
	public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
		PlantillaResguardoDTO plantilla = plantillaService.obtener(id);
		return ResponseEntity.ok()
				.contentType(DOCX)
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(plantilla.nombreFichero(), StandardCharsets.UTF_8).build().toString())
				.body(plantillaService.contenido(id));
	}

}
