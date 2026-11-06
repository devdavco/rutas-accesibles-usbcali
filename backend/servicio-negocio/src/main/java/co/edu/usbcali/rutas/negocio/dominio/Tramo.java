package co.edu.usbcali.rutas.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Arista no dirigida del grafo: une dos puntos y se recorre en ambos sentidos. */
@Entity
@Table(name = "tramo")
public class Tramo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "punto_inicial_id", nullable = false)
	private Punto puntoInicial;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "punto_final_id", nullable = false)
	private Punto puntoFinal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Condicion condicion;

	/** Metros entre los dos puntos; la calcula PostGIS con un trigger y se relee tras guardar. */
	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "longitud_m", nullable = false, insertable = false, updatable = false)
	private Double longitudMetros;

	protected Tramo() {
		// requerido por JPA
	}

	public Tramo(Punto puntoInicial, Punto puntoFinal, Condicion condicion) {
		this.puntoInicial = puntoInicial;
		this.puntoFinal = puntoFinal;
		this.condicion = condicion;
	}

	public Long getId() {
		return id;
	}

	public Punto getPuntoInicial() {
		return puntoInicial;
	}

	public Punto getPuntoFinal() {
		return puntoFinal;
	}

	public Condicion getCondicion() {
		return condicion;
	}

	public void setCondicion(Condicion condicion) {
		this.condicion = condicion;
	}

	public Double getLongitudMetros() {
		return longitudMetros;
	}

}
