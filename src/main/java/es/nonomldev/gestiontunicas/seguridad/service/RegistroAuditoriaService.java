package es.nonomldev.gestiontunicas.seguridad.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.seguridad.dto.RegistroAuditoriaDto;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAcceso;

/** Registro de inicios de sesión. Solo lo consulta el Administrador. */
public interface RegistroAuditoriaService {

	/** Lo llamará el listener de los eventos de autenticación de Spring Security. */
	void registrar(String username, ResultadoAcceso resultado, String ip);

	/** Filtros opcionales (null = sin filtro). Las fechas son días completos. */
	Page<RegistroAuditoriaDto> buscar(String username, ResultadoAcceso resultado, LocalDate desde, LocalDate hasta,
			Pageable pageable);

}
