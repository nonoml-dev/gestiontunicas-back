package es.nonomldev.gestiontunicas.hermano.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HermanoEntityTest {

	@Test
	void elNumeroDeOrdenEsElNumeroDeHermanoComoEntero() {
		HermanoEntity hermano = new HermanoEntity();
		hermano.setApellidos("García López");
		hermano.setNombre("Juan");

		hermano.setNumeroHermano(" 987 ");
		hermano.normalizarNombre();
		assertThat(hermano.getNumeroOrden()).isEqualTo(987);

		hermano.setNumeroHermano("12-A");
		hermano.normalizarNombre();
		assertThat(hermano.getNumeroOrden()).isNull();

		hermano.setNumeroHermano(null);
		hermano.normalizarNombre();
		assertThat(hermano.getNumeroOrden()).isNull();
	}

}
