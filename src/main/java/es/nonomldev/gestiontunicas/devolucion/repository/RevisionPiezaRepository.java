package es.nonomldev.gestiontunicas.devolucion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPieza;

public interface RevisionPiezaRepository extends JpaRepository<RevisionPieza, Long> {

	/** Incidencias por pieza en el historial de la túnica (solo las que no salieron correctas). */
	@Query("""
			select r from RevisionPieza r
			join fetch r.pieza p
			join fetch r.devolucion d
			where p.tunica.id = :tunicaId
			  and (r.resultado <> es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevision.CORRECTA
			       or r.defectoCapa is not null)
			order by d.fecha desc
			""")
	List<RevisionPieza> findIncidenciasDeTunica(Long tunicaId);

}
