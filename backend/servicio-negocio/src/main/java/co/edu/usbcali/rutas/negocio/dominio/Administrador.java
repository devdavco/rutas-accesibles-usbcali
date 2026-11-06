package co.edu.usbcali.rutas.negocio.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Usuario que administra puntos y tramos. Los usuarios se crean en la etapa de seguridad. */
@Entity
@Table(name = "administrador")
public class Administrador {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 254)
	private String correo;

	/** Hash de la contraseña; nunca se guarda en texto plano. */
	@Column(name = "contrasena_cifrada", nullable = false, length = 100)
	private String contrasenaCifrada;

	protected Administrador() {
		// requerido por JPA
	}

	public Administrador(String correo, String contrasenaCifrada) {
		this.correo = correo;
		this.contrasenaCifrada = contrasenaCifrada;
	}

	public Long getId() {
		return id;
	}

	public String getCorreo() {
		return correo;
	}

	public String getContrasenaCifrada() {
		return contrasenaCifrada;
	}

}
