package es.nonomldev.gestiontunicas.notificacion.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.notificacion.entity.EstadoNotificacion;
import es.nonomldev.gestiontunicas.notificacion.entity.TipoNotificacion;

public record NotificacionEmailDto(
		Long id,
		Long hermanoId,
		Long alquilerId,
		TipoNotificacion tipo,
		String destinatario,
		String asunto,
		EstadoNotificacion estado,
		Integer intentos,
		String error,
		Instant creadoEn,
		Instant enviadoEn) {
}
