package es.nonomldev.gestiontunicas.reparto.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class LiberacionAutomaticaReservasTest {

	private final RepartoService repartoService = mock(RepartoService.class);
	private final LiberacionAutomaticaReservas tarea = new LiberacionAutomaticaReservas(repartoService);

	@Test
	void compruebaAlArrancarYEnCadaCuartoDeHora() {
		when(repartoService.liberarReservasSiEmpiezaElDia3()).thenReturn(Optional.empty());

		tarea.alArrancar();
		tarea.cadaCuartoDeHora();

		verify(repartoService, times(2)).liberarReservasSiEmpiezaElDia3();
	}

	@Test
	void unFalloNoTumbaLaTareaYSeReintentaEnLaSiguiente() {
		when(repartoService.liberarReservasSiEmpiezaElDia3())
				.thenThrow(new OptimisticLockingFailureException("cambiado a la vez"))
				.thenThrow(new IllegalStateException("sin conexión"))
				.thenReturn(Optional.empty());

		assertThatCode(tarea::cadaCuartoDeHora).doesNotThrowAnyException();
		assertThatCode(tarea::cadaCuartoDeHora).doesNotThrowAnyException();
		tarea.cadaCuartoDeHora();

		verify(repartoService, times(3)).liberarReservasSiEmpiezaElDia3();
	}

}
