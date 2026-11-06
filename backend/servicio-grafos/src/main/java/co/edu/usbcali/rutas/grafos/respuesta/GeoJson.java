package co.edu.usbcali.rutas.grafos.respuesta;

import java.util.List;
import java.util.Map;

/**
 * Estructuras mínimas de GeoJSON (RFC 7946). Las coordenadas van en orden [longitud, latitud].
 */
public final class GeoJson {

	private GeoJson() {
	}

	public record ColeccionElementos(String type, List<Elemento> features) {

		public ColeccionElementos(List<Elemento> features) {
			this("FeatureCollection", List.copyOf(features));
		}

	}

	public record Elemento(String type, Geometria geometry, Map<String, Object> properties) {

		public Elemento(Geometria geometry, Map<String, Object> properties) {
			this("Feature", geometry, properties);
		}

	}

	/** Geometría Point (coordinates = [lng, lat]) o LineString (coordinates = [[lng, lat], ...]). */
	public record Geometria(String type, Object coordinates) {

		public static Geometria punto(double lat, double lng) {
			return new Geometria("Point", List.of(lng, lat));
		}

		public static Geometria linea(List<List<Double>> coordenadas) {
			return new Geometria("LineString", List.copyOf(coordenadas));
		}

	}

}
