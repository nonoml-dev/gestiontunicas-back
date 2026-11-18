package es.nonomldev.gestiontunicas.reparto.service;

import java.util.List;

import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDTO;

/**
 * Plantilla Word del resguardo que aporta el prioste. La aplicación no diseña el resguardo:
 * solo detecta las variables al subirla y avisa de las que no reconoce.
 */
public interface PlantillaResguardoService {

	PlantillaResguardoDTO subir(String nombre, String nombreFichero, byte[] contenido);

	List<PlantillaResguardoDTO> listar();

	PlantillaResguardoDTO obtener(Long id);

	/** El .docx original, para descargarlo. */
	byte[] contenido(Long id);

}
