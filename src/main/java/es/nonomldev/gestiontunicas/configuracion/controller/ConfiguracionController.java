package es.nonomldev.gestiontunicas.configuracion.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDTO;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.service.ConfiguracionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Configuración general: nombre de la Hermandad, tolerancia de medida por defecto, seguridad del inicio de
 * sesión y email. La cambia el Administrador; Prioste, Mayordomo y Secretario la consultan. El Capiller no
 * tiene acceso (matriz del análisis §3).
 */
@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

	private final ConfiguracionService configuracionService;

	/** Ordenada por clave. Con {@code prefijo} (p. ej. "reparto."), solo esa sección. */
	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'PRIOSTE', 'MAYORDOMO', 'SECRETARIO')")
	public List<ConfiguracionDTO> listar(@RequestParam(required = false) String prefijo) {
		return configuracionService.listar(prefijo);
	}

	/** Solo cambia el valor; debe encajar con el tipo del parámetro y, si lo tiene, con su rango. */
	@PutMapping("/{clave}")
	@PreAuthorize("hasRole('ADMIN')")
	public ConfiguracionDTO actualizar(@PathVariable String clave, @Valid @RequestBody ConfiguracionRequest request) {
		return configuracionService.actualizar(clave, request);
	}

}
