package es.nonomldev.gestiontunicas.importacion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDto;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDto;
import es.nonomldev.gestiontunicas.importacion.entity.Importacion;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFila;

@Mapper(config = MapStructConfig.class)
public interface ImportacionMapper {

	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "usuario", source = "usuario.username")
	ImportacionDto toDto(Importacion importacion);

	@Mapping(target = "hermanoCoincidenteId", source = "hermanoCoincidente.id")
	ImportacionFilaDto toDto(ImportacionFila fila);

	List<ImportacionFilaDto> toFilaDtos(List<ImportacionFila> filas);

}
