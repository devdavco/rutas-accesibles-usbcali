package co.edu.usbcali.rutas.negocio.grafo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usbcali.rutas.negocio.TestcontainersConfiguration;
import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import co.edu.usbcali.rutas.negocio.repositorio.PuntoRepositorio;
import co.edu.usbcali.rutas.negocio.repositorio.TramoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Verifica las restricciones del esquema. Cada prueba se revierte al terminar. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RestriccionesTramoTest {

	private static final String INSERTAR_TRAMO =
			"INSERT INTO tramo (punto_inicial_id, punto_final_id, condicion) VALUES (?, ?, ?)";

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PuntoRepositorio puntoRepositorio;

	@Autowired
	private TramoRepositorio tramoRepositorio;

	@Test
	void tramoDuplicadoEnOrdenInversoFalla() {
		// En el prototipo existe el tramo 1 → 66
		assertThat(contarTramos(1, 66)).isEqualTo(1);

		assertThatThrownBy(() -> jdbc.update(INSERTAR_TRAMO, 66, 1, "PLANO"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ux_tramo_par");
	}

	@Test
	void tramoDuplicadoEnElMismoOrdenFalla() {
		assertThatThrownBy(() -> jdbc.update(INSERTAR_TRAMO, 1, 66, "RAMPA"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ux_tramo_par");
	}

	@Test
	void tramoDeUnPuntoASiMismoFalla() {
		assertThatThrownBy(() -> jdbc.update(INSERTAR_TRAMO, 5, 5, "PLANO"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_tramo_sin_bucle");
	}

	@Test
	void condicionInvalidaFalla() {
		assertThatThrownBy(() -> jdbc.update(INSERTAR_TRAMO, 1, 49, "PENDIENTE"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_tramo_condicion");
	}

	@Test
	void borrarUnPuntoConTramosFalla() {
		assertThatThrownBy(() -> jdbc.update("DELETE FROM punto WHERE id = ?", 1))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void unCruceNoPuedeSerSeleccionable() {
		assertThatThrownBy(() -> jdbc.update("UPDATE punto SET seleccionable = TRUE WHERE id = ?", 4))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_punto_cruce_no_seleccionable");
	}

	@Test
	void laLongitudEnviadaAManoSeIgnora() {
		assertThat(contarTramos(1, 49)).isZero();

		jdbc.update("INSERT INTO tramo (punto_inicial_id, punto_final_id, condicion, longitud_m) VALUES (1, 49, 'PLANO', 999)");

		assertThat(longitudTramo(1, 49)).isNotEqualTo(999.0).isEqualTo(distanciaPostgis(1, 49));
	}

	@Test
	void laLongitudSeCalculaAlGuardarConJpa() {
		Tramo tramo = tramoRepositorio.saveAndFlush(
				new Tramo(puntoRepositorio.getReferenceById(1L), puntoRepositorio.getReferenceById(49L), Condicion.RAMPA));

		assertThat(tramo.getLongitudMetros()).isPositive().isEqualTo(distanciaPostgis(1, 49));
	}

	@Test
	void moverUnPuntoRecalculaSusTramos() {
		double antes = longitudTramo(1, 66);

		// Se desplaza la Entrada Principal unos 11 m hacia el sur
		jdbc.update("UPDATE punto SET ubicacion = ST_Translate(ubicacion, 0, -0.0001) WHERE id = 1");

		assertThat(longitudTramo(1, 66)).isNotEqualTo(antes).isEqualTo(distanciaPostgis(1, 66));
	}

	private int contarTramos(long a, long b) {
		return jdbc.queryForObject("""
				SELECT count(*) FROM tramo
				WHERE (punto_inicial_id = ? AND punto_final_id = ?) OR (punto_inicial_id = ? AND punto_final_id = ?)
				""", Integer.class, a, b, b, a);
	}

	private double longitudTramo(long inicial, long fin) {
		return jdbc.queryForObject("SELECT longitud_m FROM tramo WHERE punto_inicial_id = ? AND punto_final_id = ?",
				Double.class, inicial, fin);
	}

	private double distanciaPostgis(long a, long b) {
		return jdbc.queryForObject("""
				SELECT ST_Distance(p.ubicacion::geography, q.ubicacion::geography)
				FROM punto p, punto q WHERE p.id = ? AND q.id = ?
				""", Double.class, a, b);
	}

}
