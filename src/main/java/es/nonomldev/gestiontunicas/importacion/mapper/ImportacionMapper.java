package es.nonomldev.gestiontunicas.importacion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.hermano.mapper.HermanoMapper;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDTO;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionEntity;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionFilaEntity;

@Mapper(config = MapStructConfig.class, uses = HermanoMapper.class)
public interface ImportacionMapper {

	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "usuario", source = "usuario.username")
	ImportacionDTO toDTO(ImportacionEntity importacion);

	ImportacionFilaDTO toDTO(ImportacionFilaEntity fila);

	List<ImportacionFilaDTO> toFilaDTOs(List<ImportacionFilaEntity> filas);

}
