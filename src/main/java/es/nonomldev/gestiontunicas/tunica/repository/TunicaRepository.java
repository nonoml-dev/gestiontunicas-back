package es.nonomldev.gestiontunicas.tunica.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import jakarta.persistence.LockModeType;

public interface TunicaRepository extends JpaRepository<Tunica, Long> {

	/** Túnica del inventario con ese código (las que son propiedad de un hermano no cuentan). */
	@Query("select t from Tunica t where t.codigo = :codigo and t.estado <> es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica.PROPIEDAD_HERMANO")
	Optional<Tunica> findEnInventarioByCodigo(String codigo);

	@Query("select count(t) > 0 from Tunica t where t.codigo = :codigo and t.estado <> es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica.PROPIEDAD_HERMANO")
	boolean existeEnInventario(String codigo);

	/** Aviso al dar de alta: el código perteneció a una túnica que ahora es de un hermano. */
	boolean existsByCodigoAndEstado(String codigo, EstadoTunica estado);

	@EntityGraph(attributePaths = "piezas")
	Optional<Tunica> findWithPiezasById(Long id);

	/**
	 * Túnicas propuestas al entregar: disponibles, dentro de la tolerancia y ordenadas por cercanía
	 * a la altura del hermano.
	 */
	@Query("""
			select t from Tunica t
			where t.estado = es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica.DISPONIBLE
			  and t.medidaTotalCm between :alturaCm - :toleranciaCm and :alturaCm + :toleranciaCm
			order by abs(t.medidaTotalCm - :alturaCm), t.codigo
			""")
	List<Tunica> proponerPorAltura(int alturaCm, int toleranciaCm);

	/** Bloqueo al entregar, para que los dos puestos no asignen la misma túnica a la vez. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from Tunica t where t.id = :id")
	Optional<Tunica> findByIdParaActualizar(Long id);

	/** Inventario con filtros opcionales (null = sin filtro). */
	@Query("""
			select t from Tunica t
			where t.estado in :estados
			  and (:medidaMin is null or t.medidaTotalCm >= :medidaMin)
			  and (:medidaMax is null or t.medidaTotalCm <= :medidaMax)
			""")
	Page<Tunica> buscar(Collection<EstadoTunica> estados, Integer medidaMin, Integer medidaMax, Pageable pageable);

	long countByEstado(EstadoTunica estado);

}
