plugins {
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.flyway)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.webmvc)
	implementation(libs.flyway.database.postgresql)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.actuator.test)
	testImplementation(libs.spring.boot.starter.data.jpa.test)
	testImplementation(libs.spring.boot.starter.flyway.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.validation.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
	testRuntimeOnly(libs.junit.platform.launcher)
}
