package es.nonomldev.gestiontunicas.alquiler.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDto;
import es.nonomldev.gestiontunicas.alquiler.dto.ResguardoDto;
import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.Resguardo;
import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;

/**
 * Solo convierte a DTO: los alquileres y resguardos se crean en el servicio a partir de la
 * situación del hermano (tipo, jornada, limosna, numeración...), no copiando la petición.
 */
@Mapper(config = MapStructConfig.class)
public interface AlquilerMapper {

	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "repartoAnio", source = "reparto.anio")
	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "hermanoNombreCompleto", expression = "java(alquiler.getHermano().getApellidos() + \", \" + alquiler.getHermano().getNombre())")
	@Mapping(target = "tunicaId", source = "tunica.id")
	@Mapping(target = "tunicaCodigo", source = "tunica.codigo")
	@Mapping(target = "tunicaMedidaTotalCm", source = "tunica.medidaTotalCm")
	@Mapping(target = "jornadaId", source = "jornada.id")
	@Mapping(target = "jornadaNumero", source = "jornada.numero")
	@Mapping(target = "alquilerAnteriorId", source = "alquilerAnterior.id")
	@Mapping(target = "alquilerOrigenId", source = "alquilerOrigen.id")
	@Mapping(target = "usuarioEntrega", source = "usuarioEntrega.username")
	AlquilerDto toDto(Alquiler alquiler);

	List<AlquilerDto> toDtos(List<Alquiler> alquileres);

	@Mapping(target = "alquilerId", source = "alquiler.id")
	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "plantillaResguardoId", source = "plantillaResguardo.id")
	@Mapping(target = "usuario", source = "usuario.username")
	ResguardoDto toDto(Resguardo resguardo);

	List<ResguardoDto> toResguardoDtos(List<Resguardo> resguardos);

}
