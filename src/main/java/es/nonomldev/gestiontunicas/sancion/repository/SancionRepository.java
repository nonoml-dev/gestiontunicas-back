package es.nonomldev.gestiontunicas.sancion.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.SancionEntity;

public interface SancionRepository extends JpaRepository<SancionEntity, Long> {

	/** Con sanciones pendientes no se puede alquilar (también después del año del veto). */
	boolean existsByHermanoIdAndEstado(Long hermanoId, EstadoSancionEnum estado);

	@Query("""
			select coalesce(sum(s.importe), 0) from SancionEntity s
			where s.hermano.id = :hermanoId
			  and s.estado = es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum.PENDIENTE
			""")
	BigDecimal totalPendiente(Long hermanoId);

	/** Pestaña "Sanciones" de la ficha del hermano. */
	@EntityGraph(attributePaths = "reparto")
	List<SancionEntity> findByHermanoIdOrderByCreadoEnDesc(Long hermanoId);

	List<SancionEntity> findByAlquilerId(Long alquilerId);

	/** Evita generar dos veces el mismo concepto (p. ej. C al cerrar el plazo y otra vez al devolverla). */
	boolean existsByAlquilerIdAndConceptoAndEstadoNot(Long alquilerId, ConceptoSancionEnum concepto, EstadoSancionEnum estado);

	/** Pantalla de Sanciones (Mayordomía) con filtros opcionales (null = sin filtro). */
	@Query("""
			select s from SancionEntity s
			join fetch s.hermano h
			join fetch s.reparto r
			where (:estado is null or s.estado = :estado)
			  and (:repartoId is null or r.id = :repartoId)
			  and (:concepto is null or s.concepto = :concepto)
			""")
	Page<SancionEntity> buscar(EstadoSancionEnum estado, Long repartoId, ConceptoSancionEnum concepto, Pageable pageable);

}
