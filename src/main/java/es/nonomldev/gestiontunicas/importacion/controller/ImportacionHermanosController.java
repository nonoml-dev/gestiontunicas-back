package es.nonomldev.gestiontunicas.importacion.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDTO;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;
import es.nonomldev.gestiontunicas.importacion.service.ImportacionHermanosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Asistente de importación del Excel de Secretaría: subir → revisar → confirmar.
 * Solo para quien gestiona hermanos (Administrador, Prioste, Mayordomo y Secretario).
 */
@RestController
@RequestMapping("/api/v1/hermanos/importaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PRIOSTE', 'MAYORDOMO', 'SECRETARIO')")
public class ImportacionHermanosController {

	private static final MediaType XLSX = MediaType
			.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ImportacionHermanosService importacionService;

	/** Sube el Excel y devuelve la importación PREVISUALIZADA (aún no toca los hermanos). */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ImportacionDTO subir(@RequestParam("fichero") MultipartFile fichero,
			@RequestParam(required = false) Long repartoId) throws IOException {
		String nombre = fichero.getOriginalFilename() == null ? "hermanos.xlsx" : fichero.getOriginalFilename();
		String minusculas = nombre.toLowerCase(Locale.ROOT);
		if (fichero.isEmpty()) {
			throw new ReglaNegocioException("El fichero está vacío");
		}
		if (!minusculas.endsWith(".xlsx") && !minusculas.endsWith(".xls")) {
			throw new ReglaNegocioException("El fichero tiene que ser un Excel (.xlsx o .xls)");
		}
		try (InputStream excel = fichero.getInputStream()) {
			return importacionService.previsualizar(nombre, excel, repartoId);
		}
	}

	@GetMapping("/{id}")
	public ImportacionDTO obtener(@PathVariable Long id) {
		return importacionService.obtener(id);
	}

	/** Filas de la importación; sin {@code estado}, todas. */
	@GetMapping("/{id}/filas")
	public List<ImportacionFilaDTO> filas(@PathVariable Long id, @RequestParam(required = false) EstadoFilaEnum estado) {
		return importacionService.filas(id, estado);
	}

	/** Decisión sobre un posible duplicado: ACTUALIZAR (el existente), CREAR (uno nuevo) o IGNORAR. */
	@PutMapping("/filas/{filaId}/decision")
	public ImportacionFilaDTO decidir(@PathVariable Long filaId, @Valid @RequestBody DecisionFilaRequest request) {
		return importacionService.decidir(filaId, request);
	}

	@PostMapping("/{id}/confirmar")
	public ImportacionDTO confirmar(@PathVariable Long id) {
		return importacionService.confirmar(id);
	}

	@PostMapping("/{id}/cancelar")
	public ImportacionDTO cancelar(@PathVariable Long id) {
		return importacionService.cancelar(id);
	}

	@GetMapping("/plantilla")
	public ResponseEntity<byte[]> plantilla() {
		return ResponseEntity.ok()
				.contentType(XLSX)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("plantilla-hermanos.xlsx").build().toString())
				.body(importacionService.plantilla());
	}

}
