package co.edu.usbcali.rutas.negocio.grafo;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usbcali.rutas.negocio.TestcontainersConfiguration;
import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Punto;
import co.edu.usbcali.rutas.negocio.dominio.TipoPunto;
import co.edu.usbcali.rutas.negocio.repositorio.PuntoRepositorio;
import co.edu.usbcali.rutas.negocio.repositorio.TramoRepositorio;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifica que la carga V3 deja en la base de datos el mismo grafo del prototipo. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CargaGrafoTest {

	/** Puntos de datos.js; los tramos se cuentan desde el CSV generado. */
	private static final int PUNTOS_PROTOTIPO = 94;

	/** Diferencia máxima permitida entre la longitud de PostGIS y los metros del prototipo. */
	private static final double TOLERANCIA_METROS = 1.0;

	private final List<TramoPrototipo> tramosPrototipo = TramoPrototipo.leerTodos();

	@Autowired
	private PuntoRepositorio puntoRepositorio;

	@Autowired
	private TramoRepositorio tramoRepositorio;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cantidadDePuntosYTramosCoincideConElPrototipo() {
		assertThat(puntoRepositorio.count()).isEqualTo(PUNTOS_PROTOTIPO);
		assertThat(tramoRepositorio.count()).isEqualTo(tramosPrototipo.size()).isEqualTo(107);
	}

	@Test
	void distribucionDeCondicionesCoincideConElPrototipo() {
		Map<String, Long> esperada = tramosPrototipo.stream()
			.collect(Collectors.groupingBy(TramoPrototipo::condicion, Collectors.counting()));

		for (Condicion condicion : Condicion.values()) {
			assertThat(tramoRepositorio.countByCondicion(condicion))
				.as("tramos %s", condicion)
				.isEqualTo(esperada.getOrDefault(condicion.name(), 0L));
		}
		// Valores confirmados en datos.js: 95 planos, 11 rampas, 1 con escaleras
		assertThat(esperada).containsExactlyInAnyOrderEntriesOf(Map.of("PLANO", 95L, "RAMPA", 11L, "ESCALERAS", 1L));
	}

	@Test
	void soloLosDestinosDelAlcanceSonSeleccionables() {
		List<Punto> seleccionables = puntoRepositorio.findBySeleccionableTrue();

		assertThat(seleccionables)
			.filteredOn(p -> p.getTipo() == TipoPunto.ENTRADA_CAMPUS)
			.extracting(Punto::getNombre)
			.containsExactlyInAnyOrder("Entrada Principal", "Entrada de Estudiantes (Parqueadero)");
		assertThat(seleccionables)
			.filteredOn(p -> p.getTipo() == TipoPunto.ENTRADA_EDIFICIO)
			.extracting(Punto::getEdificio)
			.containsOnly("Los Cerezos", "Horizontes", "Biblioteca", "Farallones", "El Cedro", "El Lago",
					"Higuerones 1", "Higuerones 2", "Los Naranjos", "Parque Tecnológico")
			.hasSize(14);
		// Fuera de alcance: se cargan para conservar la conectividad, pero no se pueden elegir
		assertThat(jdbc.queryForList("SELECT DISTINCT edificio FROM punto WHERE tipo = 'ENTRADA_EDIFICIO' AND NOT seleccionable",
				String.class))
			.containsExactlyInAnyOrder("Palmas", "Capilla", "Kiosco El Lago");
	}

	@Test
	void longitudCalculadaPorPostgisCoincideConLosMetrosDelPrototipo() {
		Map<String, TramoPrototipo> prototipoPorPar = tramosPrototipo.stream()
			.collect(Collectors.toMap(TramoPrototipo::clavePar, Function.identity()));

		List<Map<String, Object>> tramosBd = jdbc.queryForList(
				"SELECT punto_inicial_id, punto_final_id, condicion, longitud_m FROM tramo");
		assertThat(tramosBd).hasSize(prototipoPorPar.size());

		double errorMaximo = 0;
		double sumaErrores = 0;
		String peorTramo = "";
		for (Map<String, Object> fila : tramosBd) {
			long inicial = ((Number) fila.get("punto_inicial_id")).longValue();
			long fin = ((Number) fila.get("punto_final_id")).longValue();
			String clave = Math.min(inicial, fin) + "-" + Math.max(inicial, fin);
			TramoPrototipo esperado = prototipoPorPar.get(clave);
			assertThat(esperado).as("el tramo %s existe en el prototipo", clave).isNotNull();
			assertThat(fila.get("condicion")).as("condición del tramo %s", clave).isEqualTo(esperado.condicion());

			double longitud = ((Number) fila.get("longitud_m")).doubleValue();
			double error = Math.abs(longitud - esperado.metros());
			sumaErrores += error;
			if (error > errorMaximo) {
				errorMaximo = error;
				peorTramo = "%s (PostGIS %.2f m, prototipo %.1f m)".formatted(clave, longitud, esperado.metros());
			}
			assertThat(error).as("diferencia en el tramo %s", clave).isLessThanOrEqualTo(TOLERANCIA_METROS);
		}

		System.out.printf("Comparación de longitudes (%d tramos): error máximo %.3f m en %s; error promedio %.3f m%n",
				tramosBd.size(), errorMaximo, peorTramo, sumaErrores / tramosBd.size());
	}

}
