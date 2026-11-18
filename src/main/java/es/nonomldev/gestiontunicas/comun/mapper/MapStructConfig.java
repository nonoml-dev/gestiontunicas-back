package es.nonomldev.gestiontunicas.comun.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Configuración común de todos los mappers de MapStruct.
 * <ul>
 * <li>Los mappers son beans de Spring.</li>
 * <li>Si un campo del destino se queda sin mapear, la compilación falla: obliga a decidir
 * explícitamente qué se ignora (id, auditoría, relaciones que rellena el servicio...).</li>
 * </ul>
 */
@MapperConfig(componentModel = MappingConstants.ComponentModel.SPRING,
		unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MapStructConfig {
}
