package es.nonomldev.gestiontunicas.alquiler.service;

import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDto;

/** Qué le toca a un hermano en el reparto abierto (análisis §4.1 y §4.2). */
public interface SituacionHermanoService {

	SituacionHermanoDto evaluar(Long hermanoId);

}
