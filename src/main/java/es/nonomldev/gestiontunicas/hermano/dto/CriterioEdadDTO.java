package es.nonomldev.gestiontunicas.hermano.dto;

import java.time.LocalDate;

/**
 * Criterio del filtro "En edad" del listado: edad cumplida a Jueves Santo entre {@code edadMin} y {@code edadMax}.
 *
 * @param juevesSanto el del reparto abierto o, si no hay ninguno, el próximo Jueves Santo.
 * @param repartoAnio año del reparto abierto; nulo si no hay ninguno (se usan 10–15 años).
 */
public record CriterioEdadDTO(LocalDate juevesSanto, int edadMin, int edadMax, Integer repartoAnio) {
}
