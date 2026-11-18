package es.nonomldev.gestiontunicas.comun.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import es.nonomldev.gestiontunicas.hermano.entity.Hermano;

/** Formatos de texto que se muestran al usuario (mensajes de motivo, resguardo...). */
public final class Formato {

	public static final Locale ES = Locale.of("es", "ES");
	public static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private Formato() {
	}

	/** "15,00 €" */
	public static String euros(BigDecimal importe) {
		NumberFormat formato = NumberFormat.getCurrencyInstance(ES);
		return formato.format(importe == null ? BigDecimal.ZERO : importe);
	}

	/** "Apellidos, Nombre", como en el Excel de Secretaría. */
	public static String nombreCompleto(Hermano hermano) {
		return hermano.getApellidos() + ", " + hermano.getNombre();
	}

}
