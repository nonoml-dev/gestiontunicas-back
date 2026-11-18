package es.nonomldev.gestiontunicas.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.nonomldev.gestiontunicas.notificacion.entity.EstadoNotificacionEnum;
import es.nonomldev.gestiontunicas.notificacion.entity.NotificacionEmailEntity;
import es.nonomldev.gestiontunicas.notificacion.entity.TipoNotificacionEnum;

public interface NotificacionEmailRepository extends JpaRepository<NotificacionEmailEntity, Long> {

	/** Envíos pendientes o con error que aún se pueden reintentar. */
	List<NotificacionEmailEntity> findByEstadoInAndIntentosLessThanOrderByCreadoEn(List<EstadoNotificacionEnum> estados, int maxIntentos);

	List<NotificacionEmailEntity> findByHermanoIdOrderByCreadoEnDesc(Long hermanoId);

	/** Evita mandar dos veces el recordatorio de devolución del mismo alquiler. */
	boolean existsByAlquilerIdAndTipo(Long alquilerId, TipoNotificacionEnum tipo);

}
