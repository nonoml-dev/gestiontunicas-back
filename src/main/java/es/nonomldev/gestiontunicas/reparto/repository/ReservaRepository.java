package es.nonomldev.gestiontunicas.reparto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva;
import es.nonomldev.gestiontunicas.reparto.entity.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

	/** Reserva del hermano que renueva (ficha del hermano). */
	Optional<Reserva> findByRepartoIdAndHermanoId(Long repartoId, Long hermanoId);

	/** ¿La túnica está reservada para otro hermano? */
	Optional<Reserva> findByRepartoIdAndTunicaIdAndEstado(Long repartoId, Long tunicaId, EstadoReserva estado);

	List<Reserva> findByRepartoIdAndEstado(Long repartoId, EstadoReserva estado);

	long countByRepartoIdAndEstado(Long repartoId, EstadoReserva estado);

	/** Al empezar el día 3: se liberan las reservas que no se han recogido. */
	@Modifying
	@Query("""
			update Reserva r set r.estado = es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva.LIBERADA,
			                     r.resueltaEn = CURRENT_TIMESTAMP
			where r.reparto.id = :repartoId
			  and r.estado = es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva.ACTIVA
			""")
	int liberarActivas(Long repartoId);

}
