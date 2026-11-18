package es.nonomldev.gestiontunicas.notificacion.service;

import java.util.List;

import es.nonomldev.gestiontunicas.notificacion.dto.NotificacionEmailDTO;

/**
 * Emails enviados a un hermano o su tutor (resguardo y recordatorio de devolución).
 * El envío real se añadirá con la configuración SMTP de Administración (tarea E7.5–E7.7).
 */
public interface NotificacionEmailService {

	List<NotificacionEmailDTO> deHermano(Long hermanoId);

}
