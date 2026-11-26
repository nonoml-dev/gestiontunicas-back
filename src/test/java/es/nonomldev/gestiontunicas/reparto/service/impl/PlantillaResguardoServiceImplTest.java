package es.nonomldev.gestiontunicas.reparto.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDTO;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapperImpl;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;

class PlantillaResguardoServiceImplTest {

	private static final Instant AHORA = Instant.parse("2026-11-26T18:00:00Z");

	private final PlantillaResguardoRepository repository = mock(PlantillaResguardoRepository.class);
	private final UsuarioActualService usuarioActual = mock(UsuarioActualService.class);
	private final PlantillaResguardoServiceImpl servicio = new PlantillaResguardoServiceImpl(repository,
			new RepartoMapperImpl(), usuarioActual, Clock.fixed(AHORA, ZoneId.of("Europe/Madrid")));

	@BeforeEach
	void preparar() {
		UsuarioEntity prioste = new UsuarioEntity();
		prioste.setUsername("prioste");
		when(usuarioActual.obtener()).thenReturn(prioste);
		when(repository.save(any())).thenAnswer(i -> i.<PlantillaResguardoEntity>getArgument(0));
	}

	@Test
	void guardaElWordConLasVariablesYQuienLoSube() throws IOException {
		byte[] word = word("Nombre: {{hermano.nombre}} {{hermano.apellidos}}", "Túnica nº {{tunica.codigo}}");

		PlantillaResguardoDTO dto = servicio.subir("Recibo 2027", "RECIBO TUNICAS 2027.docx", word);

		assertThat(dto.nombre()).isEqualTo("Recibo 2027");
		assertThat(dto.nombreFichero()).isEqualTo("RECIBO TUNICAS 2027.docx");
		assertThat(dto.variablesDetectadas()).containsExactly("hermano.nombre", "hermano.apellidos", "tunica.codigo");
		assertThat(dto.variablesDesconocidas()).isEmpty();
		assertThat(dto.subidoPor()).isEqualTo("prioste");
		assertThat(dto.subidoEn()).isEqualTo(AHORA);
	}

	@Test
	void sinNombreSeUsaElDelFichero() throws IOException {
		PlantillaResguardoDTO dto = servicio.subir(" ", "recibo.docx", word("{{tunica.codigo}}"));

		assertThat(dto.nombre()).isEqualTo("recibo.docx");
	}

	@Test
	void avisaDeLasErratasYDeLasLlavesMalCerradas() throws IOException {
		byte[] word = word("{{hermano.nombe}}", "Cíngulo: {{reparto.importe_d} €", "Talega: {{reparto.importe_b}} €");

		PlantillaResguardoDTO dto = servicio.subir(null, "recibo.docx", word);

		assertThat(dto.variablesDesconocidas()).containsExactly("hermano.nombe", "{{reparto.importe_d}");
	}

	@Test
	void detectaUnaVariablePartidaEnVariosTrozosDeTexto() throws IOException {
		XWPFDocument documento = new XWPFDocument();
		XWPFParagraph parrafo = documento.createParagraph();
		parrafo.createRun().setText("Limosna: {{alqui");
		parrafo.createRun().setText("ler.limo");
		parrafo.createRun().setText("sna}} €");

		PlantillaResguardoDTO dto = servicio.subir(null, "recibo.docx", bytes(documento));

		assertThat(dto.variablesDetectadas()).containsExactly("alquiler.limosna");
	}

	@Test
	void detectaLasVariablesDeLasTablasYDeLaCabecera() throws IOException {
		XWPFDocument documento = new XWPFDocument();
		XWPFTable tabla = documento.createTable(1, 2);
		tabla.getRow(0).getCell(0).setText("{{reparto.importe_a}}");
		tabla.getRow(0).getCell(1).setText("{{reparto.importe_f}}");
		documento.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT)
				.createParagraph().createRun().setText("Resguardo {{resguardo.numero}}");

		PlantillaResguardoDTO dto = servicio.subir(null, "recibo.docx", bytes(documento));

		assertThat(dto.variablesDetectadas())
				.containsExactlyInAnyOrder("reparto.importe_a", "reparto.importe_f", "resguardo.numero");
	}

	@ParameterizedTest
	@ValueSource(strings = { "recibo.doc", "recibo.pdf", "recibo" })
	void soloAdmiteWordDocx(String nombreFichero) throws IOException {
		byte[] word = word("{{tunica.codigo}}");

		assertThatThrownBy(() -> servicio.subir(null, nombreFichero, word))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining(".docx");
		verifyNoInteractions(repository);
	}

	@Test
	void laExtensionDaIgualEnMayusculas() throws IOException {
		assertThat(servicio.subir(null, "RECIBO.DOCX", word("{{tunica.codigo}}")).variablesDetectadas())
				.containsExactly("tunica.codigo");
	}

	@Test
	void rechazaUnFicheroVacio() {
		assertThatThrownBy(() -> servicio.subir(null, "recibo.docx", new byte[0]))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("vacío");
		verifyNoInteractions(repository);
	}

	@Test
	void rechazaUnFicheroQueNoEsUnWord() {
		byte[] noEsWord = "esto no es un docx".getBytes();

		assertThatThrownBy(() -> servicio.subir(null, "recibo.docx", noEsWord))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("No se puede leer el documento Word");
		verifyNoInteractions(repository);
	}

	private static byte[] word(String... parrafos) throws IOException {
		XWPFDocument documento = new XWPFDocument();
		for (String texto : parrafos) {
			documento.createParagraph().createRun().setText(texto);
		}
		return bytes(documento);
	}

	private static byte[] bytes(XWPFDocument documento) throws IOException {
		try (documento; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			documento.write(salida);
			return salida.toByteArray();
		}
	}

}
