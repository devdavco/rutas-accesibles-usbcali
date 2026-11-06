package co.edu.usbcali.rutas.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

/** Vértice del grafo: una entrada o un cruce del campus. */
@Entity
@Table(name = "punto")
public class Punto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoPunto tipo;

	/** Edificio al que pertenece; nulo en cruces y entradas del campus. */
	@Column(length = 60)
	private String edificio;

	/** Si se puede elegir como origen o destino de una ruta. */
	@Column(nullable = false)
	private boolean seleccionable;

	/** Coordenadas en WGS84 (SRID 4326): x = longitud, y = latitud. */
	@Column(nullable = false, columnDefinition = "geometry(Point, 4326)")
	private Point ubicacion;

	protected Punto() {
		// requerido por JPA
	}

	public Punto(String nombre, TipoPunto tipo, String edificio, boolean seleccionable, Point ubicacion) {
		this.nombre = nombre;
		this.tipo = tipo;
		this.edificio = edificio;
		this.seleccionable = seleccionable;
		this.ubicacion = ubicacion;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public TipoPunto getTipo() {
		return tipo;
	}

	public void setTipo(TipoPunto tipo) {
		this.tipo = tipo;
	}

	public String getEdificio() {
		return edificio;
	}

	public void setEdificio(String edificio) {
		this.edificio = edificio;
	}

	public boolean isSeleccionable() {
		return seleccionable;
	}

	public void setSeleccionable(boolean seleccionable) {
		this.seleccionable = seleccionable;
	}

	public Point getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(Point ubicacion) {
		this.ubicacion = ubicacion;
	}

}
