package es.nonomldev.gestiontunicas.reparto.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catálogo de variables que se pueden escribir en la plantilla Word del resguardo ({@code {{grupo.variable}}}),
 * según el análisis (§4.7).
 */
public final class VariablesResguardo {

	/** {{ variable }} con espacios opcionales. */
	public static final Pattern PATRON = Pattern.compile("\\{\\{\\s*([\\p{L}\\w.]+)\\s*}}");

	/**
	 * Lo que queda con llaves tras quitar las variables bien escritas: "{{x}", "{x}}", "{{x" sin cerrar o
	 * "x}}" sin abrir. Se toma el trozo pegado a las llaves, sin espacios.
	 */
	private static final Pattern MAL_ESCRITA = Pattern.compile("\\{+\\s*[^{}\\s]*\\s*}*|[^{}\\s]*}+");

	public static final Set<String> CATALOGO = Set.of(
			// Hermandad y fecha
			"hermandad.nombre", "fecha_hoy",
			// Hermano
			"hermano.nombre", "hermano.apellidos", "hermano.nombre_completo", "hermano.fecha_nacimiento",
			"hermano.edad", "hermano.telefono1", "hermano.telefono2", "hermano.telefonos", "hermano.email",
			// Túnica
			"tunica.codigo", "tunica.medida", "tunica.dobladillo", "tunica.medida_total",
			// Entrega
			"resguardo.numero", "resguardo.fecha_emision", "alquiler.tipo", "alquiler.fecha_entrega",
			"alquiler.jornada", "alquiler.limosna", "alquiler.entregado_por",
			// Fecha de entrega por partes: día y año en número, mes en letra ("abril")
			"alquiler.fecha_entrega_dia", "alquiler.fecha_entrega_mes", "alquiler.fecha_entrega_anio",
			// RepartoEntity
			"reparto.anio", "reparto.devolucion_inicio", "reparto.devolucion_fin", "reparto.fin_plazo_gracia",
			// Fecha límite de devolución por partes: día y año en número, mes en letra ("abril")
			"reparto.devolucion_fin_dia", "reparto.devolucion_fin_mes", "reparto.devolucion_fin_anio",
			"reparto.lugar_devolucion", "reparto.importe_a", "reparto.importe_b", "reparto.importe_c",
			"reparto.importe_d", "reparto.importe_e", "reparto.importe_f");

	private VariablesResguardo() {
	}

	/**
	 * Variables que aparecen en un texto, sin repetir y en orden de aparición. Detrás van, tal como están
	 * escritas, las que tienen las llaves mal (p. ej. "{{reparto.importe_d}"): al no estar en el
	 * catálogo, {@link #desconocidas} las señala para corregirlas antes de usar la plantilla.
	 */
	public static List<String> detectar(String texto) {
		Set<String> encontradas = new LinkedHashSet<>();
		Matcher matcher = PATRON.matcher(texto);
		while (matcher.find()) {
			encontradas.add(matcher.group(1));
		}
		Matcher malEscritas = MAL_ESCRITA.matcher(PATRON.matcher(texto).replaceAll(" "));
		while (malEscritas.find()) {
			encontradas.add(malEscritas.group().strip());
		}
		return List.copyOf(encontradas);
	}

	/** Las que no están en el catálogo (p. ej. una errata como {{hermano.nombe}}). */
	public static List<String> desconocidas(List<String> variables) {
		return variables == null ? List.of() : variables.stream().filter(v -> !CATALOGO.contains(v)).toList();
	}

}
