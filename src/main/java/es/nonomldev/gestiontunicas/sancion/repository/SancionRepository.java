package es.nonomldev.gestiontunicas.sancion.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.Sancion;

public interface SancionRepository extends JpaRepository<Sancion, Long> {

	/** Con sanciones pendientes no se puede alquilar (también después del año del veto). */
	boolean existsByHermanoIdAndEstado(Long hermanoId, EstadoSancion estado);

	@Query("""
			select coalesce(sum(s.importe), 0) from Sancion s
			where s.hermano.id = :hermanoId
			  and s.estado = es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion.PENDIENTE
			""")
	BigDecimal totalPendiente(Long hermanoId);

	/** Pestaña "Sanciones" de la ficha del hermano. */
	@EntityGraph(attributePaths = "reparto")
	List<Sancion> findByHermanoIdOrderByCreadoEnDesc(Long hermanoId);

	List<Sancion> findByAlquilerId(Long alquilerId);

	/** Evita generar dos veces el mismo concepto (p. ej. C al cerrar el plazo y otra vez al devolverla). */
	boolean existsByAlquilerIdAndConceptoAndEstadoNot(Long alquilerId, ConceptoSancion concepto, EstadoSancion estado);

	/** Pantalla de Sanciones (Mayordomía) con filtros opcionales (null = sin filtro). */
	@Query("""
			select s from Sancion s
			join fetch s.hermano h
			join fetch s.reparto r
			where (:estado is null or s.estado = :estado)
			  and (:repartoId is null or r.id = :repartoId)
			  and (:concepto is null or s.concepto = :concepto)
			""")
	Page<Sancion> buscar(EstadoSancion estado, Long repartoId, ConceptoSancion concepto, Pageable pageable);

}
