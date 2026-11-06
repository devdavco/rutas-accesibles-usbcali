plugins {
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.webmvc)
	testImplementation(libs.spring.boot.starter.actuator.test)
	testImplementation(libs.spring.boot.starter.validation.test)
	testImplementation(libs.spring.boot.starter.webmvc.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}
