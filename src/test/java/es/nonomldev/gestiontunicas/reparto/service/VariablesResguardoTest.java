package es.nonomldev.gestiontunicas.reparto.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VariablesResguardoTest {

	@Test
	void detectaLasVariablesSinRepetirYEnOrden() {
		String texto = "D. {{hermano.nombre}} {{ hermano.apellidos }}\nTúnica {{tunica.codigo}} · {{hermano.nombre}}";

		assertThat(VariablesResguardo.detectar(texto))
				.containsExactly("hermano.nombre", "hermano.apellidos", "tunica.codigo");
	}

	@Test
	void unTextoSinVariablesNoDevuelveNada() {
		assertThat(VariablesResguardo.detectar("RECIBO DE TÚNICA. Limosna: 20 €")).isEmpty();
	}

	@Test
	void unaErrataEnElNombreSaleComoDesconocida() {
		List<String> variables = VariablesResguardo.detectar("{{hermano.nombe}} {{tunica.codigo}}");

		assertThat(VariablesResguardo.desconocidas(variables)).containsExactly("hermano.nombe");
	}

	@ParameterizedTest
	@ValueSource(strings = { "{{reparto.importe_d}", "{reparto.importe_d}}", "{{reparto.importe_d", "reparto.importe_d}}",
			"{reparto.importe_d}", "{{}}" })
	void lasLlavesMalEscritasSalenComoDesconocidasTalCualEstan(String malEscrita) {
		List<String> variables = VariablesResguardo.detectar("Cíngulo: " + malEscrita + " € · Talega: {{reparto.importe_b}} €");

		assertThat(variables).containsExactly("reparto.importe_b", malEscrita);
		assertThat(VariablesResguardo.desconocidas(variables)).containsExactly(malEscrita);
	}

	@Test
	void unaVariableBienEscritaJuntoAUnaMalCerradaNoSeConfunden() {
		List<String> variables = VariablesResguardo.detectar("{{reparto.importe_c}}{{reparto.importe_d}");

		assertThat(variables).containsExactly("reparto.importe_c", "{{reparto.importe_d}");
	}

	@Test
	void lasVariablesDelReciboDeLaHermandadEstanEnElCatalogo() {
		List<String> recibo = List.of("hermano.nombre", "hermano.apellidos", "hermano.telefonos", "tunica.codigo",
				"alquiler.limosna", "reparto.devolucion_fin_dia", "reparto.devolucion_fin_mes",
				"reparto.devolucion_fin_anio", "alquiler.fecha_entrega_dia", "alquiler.fecha_entrega_mes",
				"alquiler.fecha_entrega_anio", "reparto.importe_a", "reparto.importe_b", "reparto.importe_c",
				"reparto.importe_d", "reparto.importe_e", "reparto.importe_f");

		assertThat(VariablesResguardo.desconocidas(recibo)).isEmpty();
	}

	@Test
	void sinVariablesNoHayDesconocidas() {
		assertThat(VariablesResguardo.desconocidas(null)).isEmpty();
	}

}
