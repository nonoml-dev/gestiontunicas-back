package es.nonomldev.gestiontunicas.tunica.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacion;
import es.nonomldev.gestiontunicas.tunica.entity.Reparacion;

public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {

	/** Reparaciones de todas las piezas de una túnica (pestaña "Reparaciones" e historial). */
	List<Reparacion> findByPiezaTunicaIdOrderByFechaInicioDesc(Long tunicaId);

	/** ¿Quedan reparaciones abiertas? La túnica no vuelve a estar disponible hasta terminarlas. */
	boolean existsByPiezaTunicaIdAndEstadoIn(Long tunicaId, Collection<EstadoReparacion> estados);

}
