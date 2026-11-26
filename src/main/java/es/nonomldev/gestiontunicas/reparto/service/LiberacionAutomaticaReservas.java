package es.nonomldev.gestiontunicas.reparto.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Libera sin intervención las reservas no recogidas al empezar el día 3 del reparto (análisis §4.3, P4 y N5).
 * <p>
 * Se comprueba al arrancar la aplicación y cada 15 minutos: así se liberan a las 00:00 del día 3 y, si el
 * equipo estaba apagado a esa hora, en cuanto se enciende. Solo actúa una vez por reparto: después queda
 * guardado cuándo se liberaron y las comprobaciones siguientes no hacen nada.
 * <p>
 * Se desactiva con {@code app.reservas.liberacion-automatica=false} (los tests contra la base de datos lo hacen
 * para no tocar los datos locales).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.reservas.liberacion-automatica", havingValue = "true", matchIfMissing = true)
public class LiberacionAutomaticaReservas {

	private final RepartoService repartoService;

	@EventListener(ApplicationReadyEvent.class)
	public void alArrancar() {
		comprobar();
	}

	@Scheduled(cron = "0 */15 * * * *", zone = "Europe/Madrid")
	public void cadaCuartoDeHora() {
		comprobar();
	}

	void comprobar() {
		try {
			repartoService.liberarReservasSiEmpiezaElDia3()
					.ifPresent(reparto -> log.info("Reservas del reparto de {} liberadas al empezar el día 3", reparto.anio()));
		} catch (OptimisticLockingFailureException e) {
			// Alguien ha cambiado el reparto a la vez (p. ej. las ha liberado a mano): se vuelve a mirar en la siguiente.
			log.info("El reparto ha cambiado mientras se liberaban las reservas; se comprobará de nuevo");
		} catch (RuntimeException e) {
			log.error("No se han podido liberar las reservas del día 3", e);
		}
	}

}
