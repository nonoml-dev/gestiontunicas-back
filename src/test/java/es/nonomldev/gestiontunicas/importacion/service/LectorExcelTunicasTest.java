package es.nonomldev.gestiontunicas.importacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelTunicas.FilaLeida;

class LectorExcelTunicasTest {

	private final LectorExcelTunicas lector = new LectorExcelTunicas();

	/** Mismo formato que el inventario real: cabecera "Número | Medida", número numérico y "medida+dobladillo". */
	@Test
	void leeElInventarioTalCualVieneSaltandoLaCabecera() throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet("Hoja1");
			fila(hoja, 0, "Número", "Medida");
			Row numerica = hoja.createRow(1);
			numerica.createCell(0).setCellValue(1);
			numerica.createCell(1).setCellValue("140+0");
			fila(hoja, 2, "2", "115+10");
			hoja.createRow(3); // fila vacía: se ignora
			fila(hoja, 4, "248", "");
		});

		List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(excel));

		assertThat(filas).containsExactly(
				new FilaLeida(2, "1", "140+0", "1", 140, 0, List.of()),
				new FilaLeida(3, "2", "115+10", "2", 115, 10, List.of()),
				new FilaLeida(5, "248", "", "248", null, null, List.of("Falta la medida")));
	}

	@ParameterizedTest(name = "\"{0}\" → {1} + {2}")
	@CsvSource({
			"'158 + 6', 158, 6",
			"'158', 158, 0",
			"' 120+ 4 ', 120, 4" })
	void aceptaEspaciosYMedidasSinDobladillo(String medida, int medidaCm, int dobladilloCm) throws IOException {
		FilaLeida fila = unaFila("42", medida);

		assertThat(fila.errores()).isEmpty();
		assertThat(fila.medidaCm()).isEqualTo(medidaCm);
		assertThat(fila.dobladilloCm()).isEqualTo(dobladilloCm);
	}

	@ParameterizedTest(name = "\"{0}\" → {1}")
	@CsvSource({
			"042, 42",
			"7, 7",
			"999, 999",
			"m-7, M-007",
			"M012, M-012",
			"M-123, M-123" })
	void normalizaElCodigo(String codigo, String esperado) throws IOException {
		FilaLeida fila = unaFila(codigo, "120+4");

		assertThat(fila.errores()).isEmpty();
		assertThat(fila.codigo()).isEqualTo(esperado);
	}

	@ParameterizedTest(name = "\"{0}\" / \"{1}\"")
	@CsvSource({
			"0, 158+6, 'Número no válido: 0 (del 1 al 999, o M-XXX para las de monaguillo)'",
			"1000, 158+6, 'Número no válido: 1000 (del 1 al 999, o M-XXX para las de monaguillo)'",
			"A-12, 158+6, 'Número no válido: A-12 (del 1 al 999, o M-XXX para las de monaguillo)'",
			"'', 158+6, Falta el número de la túnica",
			"42, 158-6, 'Medida no válida: 158-6 (formato medida+dobladillo, por ejemplo 158+6)'",
			"42, '1,58', 'Medida no válida: 1,58 (formato medida+dobladillo, por ejemplo 158+6)'",
			"42, 50+6, La medida debe estar entre 60 y 220 cm: 50",
			"42, 158+31, El dobladillo no puede pasar de 30 cm: 31" })
	void explicaCadaErrorDeLaFila(String codigo, String medida, String error) throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet();
			fila(hoja, 0, "1", "150+0"); // primera fila con datos: no es cabecera
			fila(hoja, 1, codigo, medida);
		});

		assertThat(lector.leer(new ByteArrayInputStream(excel)).get(1).errores()).containsExactly(error);
	}

	@Test
	void unNumeroRepetidoEnElFicheroEsUnErrorDeLaSegundaFila() throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet();
			fila(hoja, 0, "42", "150+0");
			fila(hoja, 1, "042", "152+2");
		});

		List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(excel));

		assertThat(filas.get(0).errores()).isEmpty();
		assertThat(filas.get(1).errores()).containsExactly("Número repetido en el fichero: 42");
	}

	@Test
	void soloLeeLaPrimeraHoja() throws IOException {
		byte[] excel = excel(libro -> {
			fila(libro.createSheet("Inventario"), 0, "1", "140+0");
			fila(libro.createSheet("Otra"), 0, "2", "150+0");
		});

		assertThat(lector.leer(new ByteArrayInputStream(excel))).extracting(FilaLeida::codigo).containsExactly("1");
	}

	@Test
	void unFicheroQueNoEsExcelDaUnErrorClaro() {
		assertThatThrownBy(() -> lector.leer(new ByteArrayInputStream(new byte[] { 1, 2, 3 })))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageStartingWith("No se puede leer el fichero Excel");
	}

	@Test
	void laPlantillaTieneLaCabeceraDelInventario() throws IOException {
		try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(lector.plantilla()))) {
			Row cabecera = libro.getSheetAt(0).getRow(0);
			assertThat(cabecera.getCell(0).getStringCellValue()).isEqualTo("Número");
			assertThat(cabecera.getCell(1).getStringCellValue()).isEqualTo("Medida");
		}
		// Leída tal cual, no tiene filas
		assertThatThrownBy(() -> lector.leer(new ByteArrayInputStream(lector.plantilla())).getFirst())
				.isInstanceOf(java.util.NoSuchElementException.class);
	}

	private FilaLeida unaFila(String codigo, String medida) throws IOException {
		byte[] excel = excel(libro -> fila(libro.createSheet(), 0, codigo, medida));
		return lector.leer(new ByteArrayInputStream(excel)).getFirst();
	}

	private static void fila(Sheet hoja, int numero, String... valores) {
		Row fila = hoja.createRow(numero);
		for (int i = 0; i < valores.length; i++) {
			fila.createCell(i).setCellValue(valores[i]);
		}
	}

	private static byte[] excel(Consumer<Workbook> contenido) throws IOException {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			contenido.accept(libro);
			libro.write(salida);
			return salida.toByteArray();
		}
	}

}
