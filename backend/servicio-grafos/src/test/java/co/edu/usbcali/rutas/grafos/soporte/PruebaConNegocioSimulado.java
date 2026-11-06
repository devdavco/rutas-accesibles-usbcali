package co.edu.usbcali.rutas.grafos.soporte;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas de la aplicación completa: servicio-negocio se reemplaza por un servidor
 * simulado que, por defecto, sirve el grafo del prototipo.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaConNegocioSimulado {

	protected static final GrafoPrototipo PROTOTIPO = GrafoPrototipo.cargar();

	protected static final ServicioNegocioSimulado NEGOCIO = new ServicioNegocioSimulado();

	@Autowired
	protected MockMvc mockMvc;

	@DynamicPropertySource
	static void urlNegocio(DynamicPropertyRegistry registro) {
		registro.add("rutas.negocio.url", NEGOCIO::url);
	}

	@BeforeEach
	void servirGrafoDelPrototipo() {
		NEGOCIO.responder(PROTOTIPO.json());
	}

}
