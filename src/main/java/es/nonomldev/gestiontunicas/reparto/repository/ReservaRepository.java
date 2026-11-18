package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;

public interface ReservaRepository extends JpaRepository<ReservaEntity, Long> {

	/** Reserva del hermano que renueva (ficha del hermano). */
	Optional<ReservaEntity> findByRepartoIdAndHermanoId(Long repartoId, Long hermanoId);

	/** ¿La túnica está reservada para otro hermano? */
	Optional<ReservaEntity> findByRepartoIdAndTunicaIdAndEstado(Long repartoId, Long tunicaId, EstadoReservaEnum estado);

	List<ReservaEntity> findByRepartoIdAndEstado(Long repartoId, EstadoReservaEnum estado);

	long countByRepartoIdAndEstado(Long repartoId, EstadoReservaEnum estado);

	/** Al empezar el día 3: se liberan las reservas que no se han recogido. */
	@Modifying
	@Query("""
			update ReservaEntity r set r.estado = es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum.LIBERADA,
			                     r.resueltaEn = CURRENT_TIMESTAMP
			where r.reparto.id = :repartoId
			  and r.estado = es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum.ACTIVA
			""")
	int liberarActivas(Long repartoId);

}
