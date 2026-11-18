package es.nonomldev.gestiontunicas.importacion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;

/**
 * Lee el Excel de Secretaría, que tiene formato fijo (C5):
 * <ol>
 * <li>"Apellidos, Nombre" → se separa por la primera coma.</li>
 * <li>Número de hermano (cambia con los años; solo se guarda).</li>
 * <li>Fecha de nacimiento dd/mm/aaaa (texto o celda de fecha).</li>
 * <li>Edad → no se usa: la aplicación la calcula a Jueves Santo.</li>
 * </ol>
 * Si la primera fila es una cabecera (la 3.ª columna no es una fecha), se salta.
 */
@Component
public class LectorExcelHermanos {

	private static final int COL_NOMBRE = 0;
	private static final int COL_NUMERO = 1;
	private static final int COL_FECHA = 2;
	/** Estricto: "31/02/2012" es un error, no el 29/02/2012 (el modo por defecto lo ajusta sin avisar). */
	private static final DateTimeFormatter FECHA_TEXTO = DateTimeFormatter.ofPattern("d/M/uuuu")
			.withResolverStyle(ResolverStyle.STRICT);

	/** Una fila ya interpretada. Si {@code errores} no está vacío, la fila no se puede importar. */
	public record FilaLeida(
			int numFila,
			/** Primera columna tal cual venía ("Apellidos, Nombre"), para identificar la fila si tiene errores. */
			String nombreOriginal,
			String apellidos,
			String nombre,
			String numeroHermano,
			LocalDate fechaNacimiento,
			List<String> errores) {
	}

	public List<FilaLeida> leer(InputStream excel) {
		try (Workbook libro = WorkbookFactory.create(excel)) {
			Sheet hoja = libro.getSheetAt(0);
			DataFormatter formato = new DataFormatter();
			List<FilaLeida> filas = new ArrayList<>();
			Set<String> numerosVistos = new HashSet<>();
			boolean primeraConDatos = true;

			for (Row fila : hoja) {
				String textoNombre = texto(formato, fila.getCell(COL_NOMBRE));
				String numero = texto(formato, fila.getCell(COL_NUMERO));
				Cell celdaFecha = fila.getCell(COL_FECHA);
				if (textoNombre.isEmpty() && numero.isEmpty() && texto(formato, celdaFecha).isEmpty()) {
					continue; // fila vacía
				}
				if (primeraConDatos) {
					primeraConDatos = false;
					if (esCabecera(formato, celdaFecha)) {
						continue;
					}
				}
				filas.add(interpretar(fila.getRowNum() + 1, textoNombre, numero, celdaFecha, formato, numerosVistos));
			}
			return filas;
		} catch (IOException | RuntimeException e) {
			if (e instanceof ReglaNegocioException regla) {
				throw regla;
			}
			throw new ReglaNegocioException("No se puede leer el fichero Excel: " + e.getMessage());
		}
	}

	/**
	 * Cabecera tal cual viene en el Excel de Secretaría ("LISTA 10-15 AÑOS"): Nombre ("APELLIDOS, NOMBRE"),
	 * Número, Fecha de N (fecha de nacimiento) y Edad (no se importa). Solo se lee la primera hoja.
	 */
	static final String[] CABECERA = { "Nombre", "Número", "Fecha de N", "Edad" };

	/** Excel vacío con la cabecera, para la plantilla descargable. */
	public byte[] plantilla() {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet("Hermanos");
			Font negrita = libro.createFont();
			negrita.setBold(true);
			CellStyle estiloCabecera = libro.createCellStyle();
			estiloCabecera.setFont(negrita);

			Row cabecera = hoja.createRow(0);
			for (int i = 0; i < CABECERA.length; i++) {
				Cell celda = cabecera.createCell(i);
				celda.setCellValue(CABECERA[i]);
				celda.setCellStyle(estiloCabecera);
			}
			int[] anchos = { 50, 10, 12, 8 };
			// La fecha de nacimiento como fecha de Excel, igual que en el fichero de Secretaría.
			CellStyle estiloFecha = libro.createCellStyle();
			estiloFecha.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
			hoja.setDefaultColumnStyle(COL_FECHA, estiloFecha);
			for (int i = 0; i < anchos.length; i++) {
				hoja.setColumnWidth(i, anchos[i] * 256);
			}
			hoja.createFreezePane(0, 1);
			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se ha podido generar la plantilla", e);
		}
	}

	private FilaLeida interpretar(int numFila, String textoNombre, String numero, Cell celdaFecha,
			DataFormatter formato, Set<String> numerosVistos) {
		List<String> errores = new ArrayList<>();
		String apellidos = null;
		String nombre = null;

		int coma = textoNombre.indexOf(',');
		if (coma < 0) {
			errores.add("Falta la coma entre apellidos y nombre");
		} else {
			apellidos = limpiar(textoNombre.substring(0, coma));
			nombre = limpiar(textoNombre.substring(coma + 1));
			if (apellidos.isEmpty() || nombre.isEmpty()) {
				errores.add("Faltan los apellidos o el nombre");
			}
		}

		if (!numero.isEmpty() && !numerosVistos.add(numero)) {
			errores.add("Nº de hermano repetido en el fichero: " + numero);
		}

		LocalDate fecha = null;
		String textoFecha = texto(formato, celdaFecha);
		if (!textoFecha.isEmpty()) {
			fecha = fecha(celdaFecha, formato);
			if (fecha == null) {
				errores.add("Fecha de nacimiento no válida: " + textoFecha + " (formato dd/mm/aaaa)");
			}
		}

		return new FilaLeida(numFila, textoNombre, apellidos, nombre, numero.isEmpty() ? null : numero, fecha, errores);
	}

	private static boolean esCabecera(DataFormatter formato, Cell celdaFecha) {
		return !texto(formato, celdaFecha).isEmpty() && fecha(celdaFecha, formato) == null;
	}

	private static LocalDate fecha(Cell celda, DataFormatter formato) {
		if (celda == null) {
			return null;
		}
		if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
			return celda.getLocalDateTimeCellValue().toLocalDate();
		}
		try {
			return LocalDate.parse(texto(formato, celda), FECHA_TEXTO);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static String texto(DataFormatter formato, Cell celda) {
		return celda == null ? "" : formato.formatCellValue(celda).trim();
	}

	private static String limpiar(String texto) {
		return texto.trim().replaceAll("\\s+", " ");
	}

}
