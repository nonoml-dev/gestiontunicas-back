package es.nonomldev.gestiontunicas.devolucion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPiezaEntity;

public interface RevisionPiezaRepository extends JpaRepository<RevisionPiezaEntity, Long> {

	/** Incidencias por pieza en el historial de la túnica (solo las que no salieron correctas). */
	@Query("""
			select r from RevisionPiezaEntity r
			join fetch r.pieza p
			join fetch r.devolucion d
			where p.tunica.id = :tunicaId
			  and (r.resultado <> es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevisionEnum.CORRECTA
			       or r.defectoCapa is not null)
			order by d.fecha desc
			""")
	List<RevisionPiezaEntity> findIncidenciasDeTunica(Long tunicaId);

}
