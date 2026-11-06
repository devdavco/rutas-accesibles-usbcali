package co.edu.usbcali.rutas.negocio;

import org.springframework.boot.SpringApplication;

public class TestServicioNegocioApplication {

	public static void main(String[] args) {
		SpringApplication.from(ServicioNegocioApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
