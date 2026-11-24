package es.nonomldev.gestiontunicas.importacion.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;

/**
 * Lee el Excel del inventario de túnicas, con formato fijo:
 * <ol>
 * <li>Número: código de la túnica, del 1 al 999 o M-XXX si es de monaguillo.</li>
 * <li>Medida: "medida+dobladillo" en cm, por ejemplo "158+6". Sin "+", es la medida sin dobladillo.</li>
 * </ol>
 * Si la primera fila es una cabecera (la 2.ª columna no es una medida), se salta. Solo se lee la primera hoja.
 */
@Component
public class LectorExcelTunicas {

	private static final int COL_CODIGO = 0;
	private static final int COL_MEDIDA = 1;

	/** Mismos límites que TunicaRequest. */
	static final int MEDIDA_MINIMA = 60;
	static final int MEDIDA_MAXIMA = 220;
	static final int DOBLADILLO_MAXIMO = 30;

	private static final Pattern CODIGO = Pattern.compile("^([1-9][0-9]{0,2}|M-[0-9]{3})$");
	private static final Pattern CODIGO_NUMERICO = Pattern.compile("^0*(\\d+)(\\.0+)?$");
	private static final Pattern CODIGO_MONAGUILLO = Pattern.compile("^M\\s*-?\\s*(\\d{1,3})$");
	/** "158+6", "158 + 6" o "158". */
	private static final Pattern MEDIDA = Pattern.compile("^(\\d{1,3})(?:\\s*\\+\\s*(\\d{1,3}))?$");

	/** Cabecera del fichero del inventario. */
	static final String[] CABECERA = { "Número", "Medida" };

	/** Una fila ya interpretada. Si {@code errores} no está vacío, la fila no se puede importar. */
	public record FilaLeida(
			int numFila,
			/** Las dos columnas tal cual venían, para identificar la fila si tiene errores. */
			String codigoOriginal,
			String medidaOriginal,
			/** Normalizado: "042" → "42", "m-7" → "M-007". Nulo si no es válido. */
			String codigo,
			Integer medidaCm,
			Integer dobladilloCm,
			List<String> errores) {
	}

	public List<FilaLeida> leer(InputStream excel) {
		try (Workbook libro = WorkbookFactory.create(excel)) {
			Sheet hoja = libro.getSheetAt(0);
			DataFormatter formato = new DataFormatter();
			List<FilaLeida> filas = new ArrayList<>();
			Set<String> codigosVistos = new HashSet<>();
			boolean primeraConDatos = true;

			for (Row fila : hoja) {
				String codigo = texto(formato, fila.getCell(COL_CODIGO));
				String medida = texto(formato, fila.getCell(COL_MEDIDA));
				if (codigo.isEmpty() && medida.isEmpty()) {
					continue; // fila vacía
				}
				if (primeraConDatos) {
					primeraConDatos = false;
					if (!medida.isEmpty() && !MEDIDA.matcher(medida).matches()) {
						continue; // cabecera
					}
				}
				filas.add(interpretar(fila.getRowNum() + 1, codigo, medida, codigosVistos));
			}
			return filas;
		} catch (IOException | RuntimeException e) {
			if (e instanceof ReglaNegocioException regla) {
				throw regla;
			}
			throw new ReglaNegocioException("No se puede leer el fichero Excel: " + e.getMessage());
		}
	}

	/** Excel vacío con la cabecera, para la plantilla descargable. */
	public byte[] plantilla() {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet("Túnicas");
			Font negrita = libro.createFont();
			negrita.setBold(true);
			CellStyle estiloCabecera = libro.createCellStyle();
			estiloCabecera.setFont(negrita);
			// Las dos columnas como texto: así Excel no convierte "158+6" en una fórmula ni quita ceros.
			CellStyle estiloTexto = libro.createCellStyle();
			estiloTexto.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("@"));

			Row cabecera = hoja.createRow(0);
			for (int i = 0; i < CABECERA.length; i++) {
				Cell celda = cabecera.createCell(i);
				celda.setCellValue(CABECERA[i]);
				celda.setCellStyle(estiloCabecera);
				hoja.setDefaultColumnStyle(i, estiloTexto);
				hoja.setColumnWidth(i, 14 * 256);
			}
			hoja.createFreezePane(0, 1);
			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se ha podido generar la plantilla", e);
		}
	}

	private FilaLeida interpretar(int numFila, String textoCodigo, String textoMedida, Set<String> codigosVistos) {
		List<String> errores = new ArrayList<>();

		String codigo = normalizarCodigo(textoCodigo);
		if (textoCodigo.isEmpty()) {
			errores.add("Falta el número de la túnica");
		} else if (codigo == null) {
			errores.add("Número no válido: " + textoCodigo + " (del 1 al 999, o M-XXX para las de monaguillo)");
		} else if (!codigosVistos.add(codigo)) {
			errores.add("Número repetido en el fichero: " + codigo);
		}

		Integer medida = null;
		Integer dobladillo = null;
		Matcher partes = MEDIDA.matcher(textoMedida);
		if (textoMedida.isEmpty()) {
			errores.add("Falta la medida");
		} else if (!partes.matches()) {
			errores.add("Medida no válida: " + textoMedida + " (formato medida+dobladillo, por ejemplo 158+6)");
		} else {
			medida = Integer.valueOf(partes.group(1));
			dobladillo = partes.group(2) == null ? 0 : Integer.valueOf(partes.group(2));
			if (medida < MEDIDA_MINIMA || medida > MEDIDA_MAXIMA) {
				errores.add("La medida debe estar entre %d y %d cm: %d".formatted(MEDIDA_MINIMA, MEDIDA_MAXIMA, medida));
			}
			if (dobladillo > DOBLADILLO_MAXIMO) {
				errores.add("El dobladillo no puede pasar de %d cm: %d".formatted(DOBLADILLO_MAXIMO, dobladillo));
			}
		}

		return new FilaLeida(numFila, textoCodigo, textoMedida, codigo, medida, dobladillo, errores);
	}

	/** Código en el formato de la aplicación, o nulo si no es válido. */
	static String normalizarCodigo(String texto) {
		String limpio = texto.trim().toUpperCase(Locale.ROOT);
		Matcher monaguillo = CODIGO_MONAGUILLO.matcher(limpio);
		if (monaguillo.matches()) {
			limpio = "M-" + "%03d".formatted(Integer.parseInt(monaguillo.group(1)));
		} else {
			Matcher numero = CODIGO_NUMERICO.matcher(limpio);
			if (numero.matches()) {
				limpio = numero.group(1).replaceFirst("^0+(?=\\d)", "");
			}
		}
		return CODIGO.matcher(limpio).matches() ? limpio : null;
	}

	private static String texto(DataFormatter formato, Cell celda) {
		return celda == null ? "" : formato.formatCellValue(celda).trim();
	}

}
