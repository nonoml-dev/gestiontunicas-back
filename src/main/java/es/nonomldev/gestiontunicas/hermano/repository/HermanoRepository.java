package es.nonomldev.gestiontunicas.hermano.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;

public interface HermanoRepository extends JpaRepository<HermanoEntity, Long> {

	/**
	 * Listado de hermanos: todos los activos, buscando por nombre y apellidos sin tildes ni mayúsculas,
	 * con coincidencia parcial (usa el índice trigram de nombre_normalizado).
	 * {@code textoNormalizado} debe venir ya normalizado con {@code HermanoEntity.normalizar(...)}.
	 * El orden lo da el {@code Pageable}.
	 */
	@Query("""
			select h from HermanoEntity h
			where h.activo = true
			  and h.nombreNormalizado like concat('%', :textoNormalizado, '%')
			""")
	Page<HermanoEntity> buscar(String textoNormalizado, Pageable pageable);

	/**
	 * Igual que {@link #buscar}, pero solo los que están en edad a Jueves Santo (filtro del listado):
	 * nacidos después de {@code nacidoDespuesDe} y en {@code nacidoHasta} o antes. Los que no tienen
	 * fecha de nacimiento no salen.
	 * <p>
	 * Las fechas son obligatorias: con {@code :param is null} PostgreSQL no sabe de qué tipo es una
	 * fecha y la consulta falla. Por eso hay dos métodos en vez de un filtro opcional.
	 */
	@Query("""
			select h from HermanoEntity h
			where h.activo = true
			  and h.nombreNormalizado like concat('%', :textoNormalizado, '%')
			  and h.fechaNacimiento > :nacidoDespuesDe
			  and h.fechaNacimiento <= :nacidoHasta
			""")
	Page<HermanoEntity> buscarEnEdad(String textoNormalizado, LocalDate nacidoDespuesDe, LocalDate nacidoHasta,
			Pageable pageable);

	/** Emparejamiento al reimportar el Excel (el número de hermano cambia con los años). */
	List<HermanoEntity> findByNombreNormalizadoAndFechaNacimiento(String nombreNormalizado, LocalDate fechaNacimiento);

	/** Posibles homónimos: mismo nombre normalizado, con cualquier fecha. */
	List<HermanoEntity> findByNombreNormalizado(String nombreNormalizado);

}
