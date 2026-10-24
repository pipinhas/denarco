package br.com.denarco;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Sobe o sistema inteiro: confere se as migrations rodam e se as entidades batem com as tabelas.
@SpringBootTest
@ActiveProfiles("test")
class DenarcoApplicationTests {

	@Test
	void contextLoads() {
	}

}
