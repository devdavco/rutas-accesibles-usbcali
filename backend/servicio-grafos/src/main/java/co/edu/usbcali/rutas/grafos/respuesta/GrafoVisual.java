package co.edu.usbcali.rutas.grafos.respuesta;

import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Grafo completo para que el mapa lo dibuje (etiquetas de edificios y animación del paso a
 * paso). Es solo para visualizar: los cálculos de ruta se hacen en este servicio.
 * Incluye los tramos con escaleras, que el mapa puede mostrar como excluidos.
 */
// pesoCalculado se envía como null en las escaleras (no se omite)
@JsonInclude(JsonInclude.Include.ALWAYS)
public record GrafoVisual(List<PuntoVisual> puntos, List<TramoVisual> tramos) {

	public record PuntoVisual(long id, String nombre, TipoPunto tipo, String edificio, boolean seleccionable,
			double lat, double lng) {
	}

	/**
	 * @param longitudM metros calculados por PostGIS
	 * @param pesoCalculado metros × factor de la condición; nulo para escaleras (no entra al grafo)
	 */
	public record TramoVisual(long id, long puntoInicialId, long puntoFinalId, Condicion condicion, double longitudM,
			Double pesoCalculado) {
	}

}
