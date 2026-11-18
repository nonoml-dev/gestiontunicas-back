package es.nonomldev.gestiontunicas.seguridad.service;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.seguridad.dto.RegistroAuditoriaDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.ResultadoAccesoEnum;

/** Registro de inicios de sesión. Solo lo consulta el Administrador. */
public interface RegistroAuditoriaService {

	/** Lo llamará el listener de los eventos de autenticación de Spring Security. */
	void registrar(String username, ResultadoAccesoEnum resultado, String ip);

	/** Filtros opcionales (null = sin filtro). Las fechas son días completos. */
	Page<RegistroAuditoriaDTO> buscar(String username, ResultadoAccesoEnum resultado, LocalDate desde, LocalDate hasta,
			Pageable pageable);

}
