package es.nonomldev.gestiontunicas.notificacion.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.notificacion.entity.EstadoNotificacionEnum;
import es.nonomldev.gestiontunicas.notificacion.entity.TipoNotificacionEnum;

public record NotificacionEmailDTO(
		Long id,
		Long hermanoId,
		Long alquilerId,
		TipoNotificacionEnum tipo,
		String destinatario,
		String asunto,
		EstadoNotificacionEnum estado,
		Integer intentos,
		String error,
		Instant creadoEn,
		Instant enviadoEn) {
}
