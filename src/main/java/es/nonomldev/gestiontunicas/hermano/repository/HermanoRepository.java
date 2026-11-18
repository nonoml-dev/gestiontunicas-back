package es.nonomldev.gestiontunicas.hermano.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.hermano.entity.Hermano;

public interface HermanoRepository extends JpaRepository<Hermano, Long> {

	/**
	 * Búsqueda por nombre y apellidos sin tildes ni mayúsculas, con coincidencia parcial
	 * (usa el índice trigram de nombre_normalizado).
	 * <p>
	 * {@code textoNormalizado} debe venir ya normalizado con {@code Hermano.normalizar(...)}.
	 * {@code nacidoDespuesDe}: solo los que aún no han cumplido 16 a Jueves Santo
	 * (los mayores no salen en el listado ni en el buscador). Nulo = sin filtro.
	 */
	@Query("""
			select h from Hermano h
			where h.activo = true
			  and h.nombreNormalizado like concat('%', :textoNormalizado, '%')
			  and (:nacidoDespuesDe is null or h.fechaNacimiento is null or h.fechaNacimiento > :nacidoDespuesDe)
			order by h.apellidos, h.nombre
			""")
	Page<Hermano> buscar(String textoNormalizado, LocalDate nacidoDespuesDe, Pageable pageable);

	/** Emparejamiento al reimportar el Excel (el número de hermano cambia con los años). */
	List<Hermano> findByNombreNormalizadoAndFechaNacimiento(String nombreNormalizado, LocalDate fechaNacimiento);

	/** Posibles homónimos: mismo nombre normalizado, con cualquier fecha. */
	List<Hermano> findByNombreNormalizado(String nombreNormalizado);

}
