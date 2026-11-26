package es.nonomldev.gestiontunicas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.reservas.liberacion-automatica=false")
class GestiontunicasApplicationTests {

	@Test
	void contextLoads() {
	}

}
