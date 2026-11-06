package co.edu.usbcali.rutas.negocio;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Misma imagen que infra/docker-compose.yml, para que Flyway habilite PostGIS en las pruebas
	private static final DockerImageName IMAGEN_POSTGIS = DockerImageName.parse("postgis/postgis:18-3.7")
		.asCompatibleSubstituteFor("postgres");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(IMAGEN_POSTGIS);
	}

}
