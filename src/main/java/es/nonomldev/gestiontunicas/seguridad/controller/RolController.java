package es.nonomldev.gestiontunicas.seguridad.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.nonomldev.gestiontunicas.seguridad.dto.RolDTO;
import es.nonomldev.gestiontunicas.seguridad.service.RolService;
import lombok.RequiredArgsConstructor;

/** Roles disponibles para asignar a un usuario. Son fijos: solo se consultan. */
@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RolController {

	private final RolService rolService;

	@GetMapping
	public List<RolDTO> listar() {
		return rolService.listar();
	}

}
