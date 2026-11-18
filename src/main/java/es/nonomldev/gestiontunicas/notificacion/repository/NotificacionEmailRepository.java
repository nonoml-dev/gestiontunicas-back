package es.nonomldev.gestiontunicas.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.notificacion.entity.EstadoNotificacion;
import es.nonomldev.gestiontunicas.notificacion.entity.NotificacionEmail;
import es.nonomldev.gestiontunicas.notificacion.entity.TipoNotificacion;

public interface NotificacionEmailRepository extends JpaRepository<NotificacionEmail, Long> {

	/** Envíos pendientes o con error que aún se pueden reintentar. */
	List<NotificacionEmail> findByEstadoInAndIntentosLessThanOrderByCreadoEn(List<EstadoNotificacion> estados, int maxIntentos);

	List<NotificacionEmail> findByHermanoIdOrderByCreadoEnDesc(Long hermanoId);

	/** Evita mandar dos veces el recordatorio de devolución del mismo alquiler. */
	boolean existsByAlquilerIdAndTipo(Long alquilerId, TipoNotificacion tipo);

}
