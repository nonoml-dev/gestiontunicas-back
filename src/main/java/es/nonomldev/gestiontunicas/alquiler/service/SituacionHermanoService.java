package es.nonomldev.gestiontunicas.alquiler.service;

import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDTO;

/** Qué le toca a un hermano en el reparto abierto (análisis §4.1 y §4.2). */
public interface SituacionHermanoService {

	SituacionHermanoDTO evaluar(Long hermanoId);

}
