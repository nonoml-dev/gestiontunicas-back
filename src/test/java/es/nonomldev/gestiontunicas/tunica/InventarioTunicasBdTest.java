package es.nonomldev.gestiontunicas.tunica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import es.nonomldev.gestiontunicas.tunica.service.TunicaService;
import jakarta.persistence.EntityManager;

/**
 * Casos límite del inventario contra PostgreSQL: lo que garantizan la restricción del código, el índice único
 * parcial (sql/postcreacion-esquema.sql), la columna generada de la medida total y la consulta de la propuesta.
 * <p>
 * Cada test va en una transacción que se deshace al terminar. Para no depender de lo que ya haya en la base de
 * datos (en local, el inventario de pruebas), al empezar se pasan todas las túnicas existentes a "propiedad del
 * hermano": así dejan libres sus códigos y no salen en las propuestas. También se deshace.
 */
@SpringBootTest
@Transactional
class InventarioTunicasBdTest {

	private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

	@Autowired
	private TunicaRepository tunicaRepository;

	@Autowired
	private TunicaService tunicaService;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void apartarLasTunicasExistentes() {
		jdbc.update("update tunica set estado = 'PROPIEDAD_HERMANO'");
	}

	// Formato del código: número del 1 al 999 o M-XXX (restricción ck_tunica_codigo)

	@ParameterizedTest(name = "\"{0}\" es válido")
	@ValueSource(strings = { "1", "9", "10", "99", "100", "999", "M-000", "M-001", "M-999" })
	void laBaseDeDatosAceptaLosCodigosLimite(String codigo) {
		TunicaEntity tunica = guardar(codigo, 150, 0, EstadoTunicaEnum.DISPONIBLE);

		assertThat(tunica.getId()).isNotNull();
		assertThat(tunica.esMonaguillo()).isEqualTo(codigo.startsWith("M-"));
	}

	@ParameterizedTest(name = "\"{0}\" se rechaza")
	@ValueSource(strings = { "0", "00", "01", "042", "1000", "-1", "1.5", "M-1", "M-12", "M-1234", "m-012", "M012",
			"M 012", "A-001", " 42", "42 " })
	void laBaseDeDatosRechazaCodigosFueraDeFormato(String codigo) {
		assertThatThrownBy(() -> guardar(codigo, 150, 0, EstadoTunicaEnum.DISPONIBLE))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	// Unicidad y reutilización del código (índice parcial ux_tunica_codigo_inventario)

	@Test
	void dosTunicasDelInventarioNoPuedenTenerElMismoCodigo() {
		guardar("42", 150, 0, EstadoTunicaEnum.DISPONIBLE);

		assertThatThrownBy(() -> guardar("42", 160, 0, EstadoTunicaEnum.EN_REPARACION))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unaTunicaDeBajaSigueOcupandoSuCodigo() {
		guardar("42", 150, 0, EstadoTunicaEnum.BAJA);

		assertThat(tunicaRepository.existeEnInventario("42")).isTrue();
		assertThatThrownBy(() -> guardar("42", 160, 0, EstadoTunicaEnum.DISPONIBLE))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void cuandoPasaASerDelHermanoSuCodigoQuedaLibreParaUnaNueva() {
		TunicaEntity antigua = guardar("42", 150, 0, EstadoTunicaEnum.NO_DEVUELTA);
		assertThat(tunicaRepository.existeEnInventario("42")).isTrue();

		antigua.setEstado(EstadoTunicaEnum.PROPIEDAD_HERMANO);
		entityManager.flush();
		assertThat(tunicaRepository.existeEnInventario("42")).isFalse();
		assertThat(tunicaRepository.existsByCodigoAndEstado("42", EstadoTunicaEnum.PROPIEDAD_HERMANO)).isTrue();

		TunicaEntity nueva = guardar("42", 160, 4, EstadoTunicaEnum.DISPONIBLE);

		// Cada una con su id (historiales separados); en el inventario solo cuenta la nueva.
		assertThat(nueva.getId()).isNotEqualTo(antigua.getId());
		assertThat(tunicaRepository.findEnInventarioByCodigo("42")).get()
				.extracting(TunicaEntity::getId).isEqualTo(nueva.getId());
		// La nueva ocupa el código: no se puede dar de alta otra.
		assertThatThrownBy(() -> guardar("42", 170, 0, EstadoTunicaEnum.DISPONIBLE))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void variasTunicasDeHermanosPuedenHaberTenidoElMismoCodigo() {
		guardar("42", 150, 0, EstadoTunicaEnum.PROPIEDAD_HERMANO);
		guardar("42", 155, 0, EstadoTunicaEnum.PROPIEDAD_HERMANO);

		assertThat(tunicaRepository.existeEnInventario("42")).isFalse();
	}

	@Test
	void alDarDeAltaElServicioReutilizaElCodigoYSoloAvisa() {
		guardar("42", 150, 0, EstadoTunicaEnum.PROPIEDAD_HERMANO);
		String nuncaUsado = codigoNuncaUsado();

		assertThat(tunicaService.codigoFueDeHermano("42")).isTrue();
		assertThat(tunicaService.codigoFueDeHermano(nuncaUsado)).isFalse();

		var creada = tunicaService.crear(new TunicaRequest("42", 158, 6, HOY, null));

		assertThat(creada.estado()).isEqualTo(EstadoTunicaEnum.DISPONIBLE);
		assertThat(creada.piezas()).hasSize(5);
		assertThatThrownBy(() -> tunicaService.crear(new TunicaRequest("42", 160, 0, HOY, null)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessage("Ya hay una túnica en el inventario con el código 42");
	}

	// Medida total (columna generada) y propuesta por tolerancia

	@Test
	void laMedidaTotalLaCalculaLaBaseDeDatos() {
		TunicaEntity tunica = guardar("42", 158, 6, EstadoTunicaEnum.DISPONIBLE);
		entityManager.refresh(tunica);

		assertThat(tunica.getMedidaTotalCm()).isEqualTo(164);

		tunica.setDobladilloCm(10);
		entityManager.flush();
		entityManager.refresh(tunica);
		assertThat(tunica.getMedidaTotalCm()).isEqualTo(168);
	}

	@Test
	void laToleranciaIncluyeLosLimitesYNadaMas() {
		guardar("1", 160, 0, EstadoTunicaEnum.DISPONIBLE); // -4: fuera
		guardar("2", 161, 0, EstadoTunicaEnum.DISPONIBLE); // -3: límite
		guardar("3", 164, 0, EstadoTunicaEnum.DISPONIBLE); // exacta
		guardar("4", 167, 0, EstadoTunicaEnum.DISPONIBLE); // +3: límite
		guardar("5", 168, 0, EstadoTunicaEnum.DISPONIBLE); // +4: fuera

		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 3))).containsExactly("3", "2", "4");
		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 0))).containsExactly("3");
		// Ampliar la búsqueda las incluye
		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 4))).containsExactly("3", "2", "4", "1", "5");
	}

	@Test
	void seComparaConLaMedidaTotalNoConLaMedida() {
		guardar("1", 158, 6, EstadoTunicaEnum.DISPONIBLE); // total 164
		guardar("2", 164, 6, EstadoTunicaEnum.DISPONIBLE); // medida 164, total 170

		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 3))).containsExactly("1");
	}

	@ParameterizedTest(name = "una túnica {0} no se propone")
	@EnumSource(value = EstadoTunicaEnum.class, names = "DISPONIBLE", mode = EnumSource.Mode.EXCLUDE)
	void soloSeProponenLasDisponibles(EstadoTunicaEnum estado) {
		guardar("1", 164, 0, estado);
		guardar("2", 164, 0, EstadoTunicaEnum.DISPONIBLE);

		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 3))).containsExactly("2");
	}

	@Test
	void aIgualDistanciaOrdenaPorCodigoComoNumeroYLasDeMonaguilloDetras() {
		guardar("M-001", 162, 0, EstadoTunicaEnum.DISPONIBLE); // -2
		guardar("10", 166, 0, EstadoTunicaEnum.DISPONIBLE); // +2
		guardar("9", 162, 0, EstadoTunicaEnum.DISPONIBLE); // -2
		guardar("100", 165, 0, EstadoTunicaEnum.DISPONIBLE); // +1

		assertThat(codigos(tunicaRepository.proponerPorAltura(164, 3))).containsExactly("100", "9", "10", "M-001");
	}

	@Test
	void sinNingunaDentroDeLaToleranciaNoSeProponeNada() {
		guardar("1", 130, 0, EstadoTunicaEnum.DISPONIBLE);

		assertThat(tunicaRepository.proponerPorAltura(164, 3)).isEmpty();
		assertThat(tunicaService.proponer(164, 3)).isEmpty();
		assertThat(tunicaService.proponer(164, 34)).hasSize(1);
	}

	private TunicaEntity guardar(String codigo, int medida, int dobladillo, EstadoTunicaEnum estado) {
		TunicaEntity tunica = new TunicaEntity();
		tunica.setCodigo(codigo);
		tunica.setMedidaCm(medida);
		tunica.setDobladilloCm(dobladillo);
		tunica.setEstado(estado);
		tunica.setFechaAlta(HOY);
		tunica.crearPiezas();
		return tunicaRepository.saveAndFlush(tunica);
	}

	/** Un código numérico que ninguna túnica ha tenido nunca (en local puede haber datos de pruebas). */
	private String codigoNuncaUsado() {
		Set<String> usados = jdbc.queryForList("select codigo from tunica", String.class).stream()
				.collect(Collectors.toSet());
		return IntStream.rangeClosed(1, 999).mapToObj(String::valueOf)
				.filter(codigo -> !usados.contains(codigo))
				.reduce((primero, ultimo) -> ultimo)
				.orElseThrow();
	}

	private static List<String> codigos(List<TunicaEntity> tunicas) {
		return tunicas.stream().map(TunicaEntity::getCodigo).toList();
	}

}
