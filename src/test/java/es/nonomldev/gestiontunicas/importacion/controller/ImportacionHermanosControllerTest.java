package es.nonomldev.gestiontunicas.importacion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import es.nonomldev.gestiontunicas.comun.config.WebConfig;
import es.nonomldev.gestiontunicas.comun.exception.ManejadorErrores;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.service.ImportacionHermanosService;
import es.nonomldev.gestiontunicas.seguridad.config.SecurityConfig;

@WebMvcTest(ImportacionHermanosController.class)
@Import({ SecurityConfig.class, WebConfig.class, ManejadorErrores.class })
@TestPropertySource(properties = {
		"app.seguridad.jwt.secret=clave-de-pruebas-de-al-menos-32-caracteres",
		"app.seguridad.jwt.duracion=PT15M",
		"app.seguridad.jwt.duracion-refresh=PT12H" })
class ImportacionHermanosControllerTest {

	private static final MockMultipartFile EXCEL = new MockMultipartFile("fichero", "hermanos 2027.xlsx",
			"application/octet-stream", new byte[] { 1, 2, 3 });

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ImportacionHermanosService importacionService;

	@Test
	void subirElExcelDevuelveLaPrevisualizacion() throws Exception {
		when(importacionService.previsualizar(eq("hermanos 2027.xlsx"), any(), isNull()))
				.thenReturn(new ImportacionDTO(7L, TipoImportacionEnum.HERMANOS, null, "hermanos 2027.xlsx",
						EstadoImportacionEnum.PREVISUALIZADA, 3, 2, 1, 0, "admin", Instant.now(), null));

		mvc.perform(multipart("/api/v1/hermanos/importaciones").file(EXCEL).with(rol("PRIOSTE")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.filasError").value(1));
	}

	@Test
	void rechazaLosFicherosQueNoSonExcel() throws Exception {
		MockMultipartFile pdf = new MockMultipartFile("fichero", "hermanos.pdf", "application/pdf", new byte[] { 1 });

		mvc.perform(multipart("/api/v1/hermanos/importaciones").file(pdf).with(rol("SECRETARIO")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("El fichero tiene que ser un Excel (.xlsx o .xls)"));
		verifyNoInteractions(importacionService);
	}

	@Test
	void elCapillerNoPuedeImportar() throws Exception {
		mvc.perform(multipart("/api/v1/hermanos/importaciones").file(EXCEL).with(rol("CAPILLER")))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/hermanos/importaciones/plantilla").with(rol("CAPILLER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void laPlantillaSeDescargaComoXlsx() throws Exception {
		when(importacionService.plantilla()).thenReturn(new byte[] { 9 });

		mvc.perform(get("/api/v1/hermanos/importaciones/plantilla").with(rol("MAYORDOMO")))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"plantilla-hermanos.xlsx\""));
	}

	private static RequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}

}
