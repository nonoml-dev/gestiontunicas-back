package es.nonomldev.gestiontunicas.importacion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.importacion.service.LectorExcelHermanos.FilaLeida;

class LectorExcelHermanosTest {

	private final LectorExcelHermanos lector = new LectorExcelHermanos();

	@Test
	void leeElFormatoDeSecretariaSaltandoLaCabeceraYSeparandoApellidosYNombre() throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet();
			fila(hoja, 0, "Apellidos, Nombre", "Nº", "Fecha de nacimiento", "Edad");
			fila(hoja, 1, "García  López, Juan", "1234", "10/05/2014", "12");
			// Fecha como celda de fecha de Excel
			Row conFecha = hoja.createRow(2);
			conFecha.createCell(0).setCellValue("Martín Ruiz, Lucía");
			conFecha.createCell(1).setCellValue(987);
			CellStyle estilo = libro.createCellStyle();
			estilo.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));
			conFecha.createCell(2).setCellValue(LocalDate.of(2012, 11, 2));
			conFecha.getCell(2).setCellStyle(estilo);
			hoja.createRow(3); // fila vacía: se ignora
			fila(hoja, 4, "Pérez Sánchez, Pablo", "", "", "");
		});

		List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(excel));

		assertThat(filas).hasSize(3);
		assertThat(filas.get(0)).isEqualTo(new FilaLeida(2, "García  López, Juan", "García López", "Juan", "1234", LocalDate.of(2014, 5, 10), List.of()));
		assertThat(filas.get(1).numeroHermano()).isEqualTo("987");
		assertThat(filas.get(1).fechaNacimiento()).isEqualTo(LocalDate.of(2012, 11, 2));
		// Sin fecha no es un error: se importa como ficha incompleta
		assertThat(filas.get(2).fechaNacimiento()).isNull();
		assertThat(filas.get(2).errores()).isEmpty();
	}

	/** Mismo formato que "LISTA 10-15 AÑOS 2026.xlsx" de Secretaría (con datos inventados). */
	@Test
	void leeElFicheroDeSecretariaTalCualViene() throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet("Hoja1");
			fila(hoja, 0, "Nombre                                            ", "Número", "Fecha de N", "Edad");
			CellStyle fechaUs = libro.createCellStyle();
			fechaUs.setDataFormat(libro.getCreationHelper().createDataFormat().getFormat("m/d/yy"));
			Row datos = hoja.createRow(1);
			datos.createCell(0).setCellValue("GARCIA-PEREZ DE LA TORRE, MIGUEl");
			datos.createCell(1).setCellValue(2396);
			datos.createCell(2).setCellValue(LocalDate.of(2011, 1, 3));
			datos.getCell(2).setCellStyle(fechaUs);
			datos.createCell(3).setCellValue(15);
			for (int i = 4; i < 26; i++) {
				datos.createCell(i); // celdas en blanco hasta la Z, como en el original
			}
			// Una segunda hoja con otras filas: no se lee
			Sheet otra = libro.createSheet("2016");
			fila(otra, 0, "Nombre", "Número", "Fecha de N", "Edad");
			fila(otra, 1, "LOPEZ RUIZ, ANA", "1818", "30/03/2016", "9");
		});

		List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(excel));

		assertThat(filas).singleElement().satisfies(f -> {
			assertThat(f.apellidos()).isEqualTo("GARCIA-PEREZ DE LA TORRE");
			assertThat(f.nombre()).isEqualTo("MIGUEl");
			assertThat(f.numeroHermano()).isEqualTo("2396");
			// "m/d/yy" es solo el formato de visualización: la fecha es el 3 de enero, no el 1 de marzo
			assertThat(f.fechaNacimiento()).isEqualTo(LocalDate.of(2011, 1, 3));
			assertThat(f.errores()).isEmpty();
		});
	}

	@Test
	void marcaLosErroresDeCadaFila() throws IOException {
		byte[] excel = excel(libro -> {
			Sheet hoja = libro.createSheet();
			fila(hoja, 0, "García López Juan", "1", "10/05/2014", "");
			fila(hoja, 1, "Martín Ruiz, Lucía", "1", "31/02/2012", "");
		});

		List<FilaLeida> filas = lector.leer(new ByteArrayInputStream(excel));

		assertThat(filas.get(0).errores()).containsExactly("Falta la coma entre apellidos y nombre");
		assertThat(filas.get(0).nombreOriginal()).isEqualTo("García López Juan");
		assertThat(filas.get(1).errores()).containsExactly("Nº de hermano repetido en el fichero: 1",
				"Fecha de nacimiento no válida: 31/02/2012 (formato dd/mm/aaaa)");
	}

	@Test
	void unFicheroQueNoEsExcelDaUnErrorClaro() {
		assertThatThrownBy(() -> lector.leer(new ByteArrayInputStream("no soy un excel".getBytes())))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageStartingWith("No se puede leer el fichero Excel");
	}

	@Test
	void laPlantillaSoloTieneLaCabeceraYSeLeeSinFilas() {
		byte[] plantilla = lector.plantilla();

		assertThat(lector.leer(new ByteArrayInputStream(plantilla))).isEmpty();
	}

	private interface Rellenar {
		void en(XSSFWorkbook libro);
	}

	private static byte[] excel(Rellenar rellenar) throws IOException {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			rellenar.en(libro);
			libro.write(salida);
			return salida.toByteArray();
		}
	}

	private static void fila(Sheet hoja, int numero, String... valores) {
		Row fila = hoja.createRow(numero);
		for (int i = 0; i < valores.length; i++) {
			fila.createCell(i).setCellValue(valores[i]);
		}
	}

}
