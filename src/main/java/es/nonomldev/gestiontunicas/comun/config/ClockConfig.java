package es.nonomldev.gestiontunicas.comun.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación en hora de España. Los servicios calculan "hoy" con este bean
 * (plazos, jornadas, edades), así los tests pueden fijar la fecha.
 */
@Configuration
public class ClockConfig {

	public static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

	@Bean
	Clock clock() {
		return Clock.system(ZONA);
	}

}
