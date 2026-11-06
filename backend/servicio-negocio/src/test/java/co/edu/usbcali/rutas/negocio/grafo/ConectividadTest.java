package co.edu.usbcali.rutas.negocio.grafo;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usbcali.rutas.negocio.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica que los destinos del alcance se pueden alcanzar sin escaleras desde la entrada principal.
 * Es solo una búsqueda de conectividad, no calcula rutas (Dijkstra va en servicio-grafos).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ConectividadTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void todosLosDestinosSeleccionablesSonAlcanzablesSinEscaleras() {
		List<String> noAlcanzados = jdbc.queryForList("""
				WITH RECURSIVE
				    -- Cada tramo utilizable se toma en ambos sentidos (grafo no dirigido)
				    aristas (desde, hasta) AS (
				        SELECT punto_inicial_id, punto_final_id FROM tramo WHERE condicion <> 'ESCALERAS'
				        UNION ALL
				        SELECT punto_final_id, punto_inicial_id FROM tramo WHERE condicion <> 'ESCALERAS'),
				    -- Búsqueda en anchura: UNION descarta los puntos ya visitados y la recursión termina
				    alcanzados (id) AS (
				        SELECT id FROM punto WHERE nombre = 'Entrada Principal'
				        UNION
				        SELECT a.hasta FROM alcanzados r JOIN aristas a ON a.desde = r.id)
				SELECT nombre FROM punto
				WHERE seleccionable AND id NOT IN (SELECT id FROM alcanzados)
				""", String.class);

		assertThat(jdbc.queryForObject("SELECT count(*) FROM punto WHERE seleccionable", Integer.class)).isEqualTo(16);
		assertThat(noAlcanzados).isEmpty();
	}

}
