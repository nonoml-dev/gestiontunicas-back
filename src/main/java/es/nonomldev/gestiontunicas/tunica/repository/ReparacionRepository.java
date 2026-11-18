package es.nonomldev.gestiontunicas.tunica.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.ReparacionEntity;

public interface ReparacionRepository extends JpaRepository<ReparacionEntity, Long> {

	/** Reparaciones de todas las piezas de una túnica (pestaña "Reparaciones" e historial). */
	List<ReparacionEntity> findByPiezaTunicaIdOrderByFechaInicioDesc(Long tunicaId);

	/** ¿Quedan reparaciones abiertas? La túnica no vuelve a estar disponible hasta terminarlas. */
	boolean existsByPiezaTunicaIdAndEstadoIn(Long tunicaId, Collection<EstadoReparacionEnum> estados);

}
