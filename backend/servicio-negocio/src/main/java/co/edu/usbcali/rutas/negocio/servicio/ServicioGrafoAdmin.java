package co.edu.usbcali.rutas.negocio.servicio;

import co.edu.usbcali.rutas.negocio.dominio.Punto;
import co.edu.usbcali.rutas.negocio.dominio.TipoPunto;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import co.edu.usbcali.rutas.negocio.repositorio.PuntoRepositorio;
import co.edu.usbcali.rutas.negocio.repositorio.TramoRepositorio;
import co.edu.usbcali.rutas.negocio.web.admin.PuntoAdminDto;
import co.edu.usbcali.rutas.negocio.web.admin.Solicitudes;
import co.edu.usbcali.rutas.negocio.web.admin.TramoAdminDto;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edición del grafo por el administrador. Las reglas se validan aquí, antes de escribir, para
 * responder con mensajes claros; la base de datos tiene además sus propias restricciones.
 * Las longitudes de los tramos las calcula PostGIS (triggers de la migración V2).
 */
@Service
@Transactional
public class ServicioGrafoAdmin {

	/** Coordenadas en WGS84 (SRID 4326). */
	private static final GeometryFactory GEOMETRIAS = new GeometryFactory(new PrecisionModel(), 4326);

	private final PuntoRepositorio puntos;

	private final TramoRepositorio tramos;

	public ServicioGrafoAdmin(PuntoRepositorio puntos, TramoRepositorio tramos) {
		this.puntos = puntos;
		this.tramos = tramos;
	}

	// --- Puntos ----------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<PuntoAdminDto> listarPuntos() {
		return puntos.findAll(Sort.by("id")).stream().map(PuntoAdminDto::desde).toList();
	}

	public PuntoAdminDto crearPunto(Solicitudes.Punto datos) {
		validarPunto(datos, null);
		Punto punto = new Punto(datos.nombre().trim(), datos.tipo(), edificio(datos), datos.seleccionable(),
				ubicacion(datos));
		return PuntoAdminDto.desde(puntos.saveAndFlush(punto));
	}

	/** Si la ubicación cambia, el trigger de PostGIS recalcula las longitudes de sus tramos. */
	public PuntoAdminDto editarPunto(Long id, Solicitudes.Punto datos) {
		Punto punto = buscarPunto(id);
		validarPunto(datos, id);
		punto.setNombre(datos.nombre().trim());
		punto.setTipo(datos.tipo());
		punto.setEdificio(edificio(datos));
		punto.setSeleccionable(datos.seleccionable());
		punto.setUbicacion(ubicacion(datos));
		return PuntoAdminDto.desde(puntos.saveAndFlush(punto));
	}

	public void eliminarPunto(Long id) {
		Punto punto = buscarPunto(id);
		long cantidad = tramos.contarDelPunto(id);
		if (cantidad > 0) {
			throw new ConflictoException("El punto «%s» tiene %d %s; elimínelos primero."
				.formatted(punto.getNombre(), cantidad, cantidad == 1 ? "tramo" : "tramos"));
		}
		puntos.delete(punto);
	}

	// --- Tramos ----------------------------------------------------------------------------

	@Transactional(readOnly = true)
	public List<TramoAdminDto> listarTramos() {
		return tramos.listarParaGrafo().stream().map(TramoAdminDto::desde).toList();
	}

	public TramoAdminDto crearTramo(Solicitudes.NuevoTramo datos) {
		if (datos.puntoInicialId().equals(datos.puntoFinalId())) {
			throw new SolicitudInvalidaException("Un tramo debe unir dos puntos distintos.");
		}
		Punto inicial = buscarPunto(datos.puntoInicialId());
		Punto fin = buscarPunto(datos.puntoFinalId());
		if (tramos.existeEntre(inicial.getId(), fin.getId())) {
			throw new ConflictoException("Ya existe un tramo entre «%s» y «%s»."
				.formatted(inicial.getNombre(), fin.getNombre()));
		}
		// saveAndFlush: la longitud la pone el trigger al insertar y Hibernate la vuelve a leer
		return TramoAdminDto.desde(tramos.saveAndFlush(new Tramo(inicial, fin, datos.condicion())));
	}

	public TramoAdminDto cambiarCondicion(Long id, Solicitudes.CambioCondicion datos) {
		Tramo tramo = buscarTramo(id);
		tramo.setCondicion(datos.condicion());
		return TramoAdminDto.desde(tramos.saveAndFlush(tramo));
	}

	public void eliminarTramo(Long id) {
		tramos.delete(buscarTramo(id));
	}

	// --- Reglas ----------------------------------------------------------------------------

	private void validarPunto(Solicitudes.Punto datos, Long idActual) {
		String nombre = datos.nombre().trim();
		boolean repetido = idActual == null ? puntos.existsByNombre(nombre)
				: puntos.existsByNombreAndIdNot(nombre, idActual);
		if (repetido) {
			throw new ConflictoException("Ya existe un punto llamado «%s».".formatted(nombre));
		}
		if (datos.tipo() == TipoPunto.CRUCE && datos.seleccionable()) {
			throw new SolicitudInvalidaException("Un cruce no se puede elegir como origen o destino.");
		}
		if (datos.tipo() == TipoPunto.ENTRADA_EDIFICIO && edificio(datos) == null) {
			throw new SolicitudInvalidaException("Una entrada de edificio debe indicar el edificio.");
		}
		if (datos.tipo() == TipoPunto.ENTRADA_CAMPUS && edificio(datos) != null) {
			throw new SolicitudInvalidaException("Una entrada del campus no pertenece a ningún edificio.");
		}
	}

	private static String edificio(Solicitudes.Punto datos) {
		return datos.edificio() == null || datos.edificio().isBlank() ? null : datos.edificio().trim();
	}

	private static org.locationtech.jts.geom.Point ubicacion(Solicitudes.Punto datos) {
		return GEOMETRIAS.createPoint(new Coordinate(datos.lng(), datos.lat()));
	}

	private Punto buscarPunto(Long id) {
		return puntos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El punto %d no existe.".formatted(id)));
	}

	private Tramo buscarTramo(Long id) {
		return tramos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El tramo %d no existe.".formatted(id)));
	}

}
