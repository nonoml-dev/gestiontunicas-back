package es.nonomldev.gestiontunicas.reparto.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reparto anual de túnicas (uno por año). Solo puede haber uno ABIERTO (índice parcial en SQL). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reparto",
		uniqueConstraints = @UniqueConstraint(name = "ux_reparto_anio", columnNames = "anio"),
		check = {
			@CheckConstraint(name = "ck_reparto_devolucion", constraint = "devolucion_fin >= devolucion_inicio"),
			@CheckConstraint(name = "ck_reparto_edades", constraint = "edad_max >= edad_min"),
			@CheckConstraint(name = "ck_reparto_plazo_gracia", constraint = "plazo_gracia_meses >= 0")
		})
public class RepartoEntity extends BaseAuditableEntity {

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "anio", nullable = false)
	private Integer anio;

	@Column(name = "fecha_jueves_santo", nullable = false)
	private LocalDate fechaJuevesSanto;

	/** true si el Jueves Santo se fijó a mano en lugar de calcularlo. */
	@Column(name = "jueves_santo_manual", nullable = false)
	private boolean juevesSantoManual;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "edad_min", nullable = false)
	private Integer edadMin = 10;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "edad_max", nullable = false)
	private Integer edadMax = 15;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoRepartoEnum estado = EstadoRepartoEnum.BORRADOR;

	/** Columnas devolucion_inicio, devolucion_fin y plazo_gracia_meses. */
	@Embedded
	private PeriodoDevolucionEntity periodoDevolucion = new PeriodoDevolucionEntity();

	@Column(name = "lugar_devolucion", length = 255)
	private String lugarDevolucion;

	@Column(name = "limosna", nullable = false, precision = 8, scale = 2)
	private BigDecimal limosna;

	/** Columnas importe_* (A–F). */
	@Embedded
	private TarifaSancionesEntity tarifas = new TarifaSancionesEntity();

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "tolerancia_medida_cm", nullable = false)
	private Integer toleranciaMedidaCm = 3;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plantilla_resguardo_id", foreignKey = @ForeignKey(name = "fk_reparto_plantilla_resguardo"))
	private PlantillaResguardoEntity plantillaResguardo;

	/** Solo el reparto de 2027: renovaciones declaradas con el resguardo en papel de 2026. */
	@Column(name = "modo_transicion", nullable = false)
	private boolean modoTransicion;

	@Column(name = "ultimo_num_resguardo", nullable = false)
	private int ultimoNumResguardo;

	@Column(name = "fecha_apertura")
	private Instant fechaApertura;

	@Column(name = "reservas_liberadas_en")
	private Instant reservasLiberadasEn;

	@Column(name = "fecha_cierre")
	private Instant fechaCierre;

	@Column(name = "no_devueltas_procesadas_en")
	private Instant noDevueltasProcesadasEn;

	@Column(name = "fin_gracia_procesado_en")
	private Instant finGraciaProcesadoEn;

	@OneToMany(mappedBy = "reparto", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("numero")
	private List<JornadaEntity> jornadas = new ArrayList<>();

	public void addJornada(JornadaEntity jornada) {
		jornada.setReparto(this);
		jornadas.add(jornada);
	}

	public Optional<JornadaEntity> jornadaDe(LocalDate fecha) {
		return jornadas.stream().filter(j -> j.getFecha().equals(fecha)).findFirst();
	}

	public boolean edadPermitida(HermanoEntity hermano) {
		if (!hermano.fichaCompleta()) {
			return false;
		}
		int edad = hermano.edadA(fechaJuevesSanto);
		return edad >= edadMin && edad <= edadMax;
	}

	public int siguienteNumeroResguardo() {
		return ++ultimoNumResguardo;
	}

	/** Jueves Santo del año: Domingo de Resurrección − 3 días (algoritmo de Meeus/Jones/Butcher). */
	public static LocalDate calcularJuevesSanto(int anio) {
		int a = anio % 19;
		int b = anio / 100;
		int c = anio % 100;
		int d = b / 4;
		int e = b % 4;
		int f = (b + 8) / 25;
		int g = (b - f + 1) / 3;
		int h = (19 * a + b - d - g + 15) % 30;
		int i = c / 4;
		int k = c % 4;
		int l = (32 + 2 * e + 2 * i - h - k) % 7;
		int m = (a + 11 * h + 22 * l) / 451;
		int mes = (h + l - 7 * m + 114) / 31;
		int dia = ((h + l - 7 * m + 114) % 31) + 1;
		return LocalDate.of(anio, mes, dia).minusDays(3);
	}

}
