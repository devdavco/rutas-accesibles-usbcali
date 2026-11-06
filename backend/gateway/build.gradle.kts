plugins {
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.cloud.starter.gateway.server.webmvc)
	testImplementation(libs.spring.boot.starter.actuator.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

dependencyManagement {
	imports {
		mavenBom(libs.spring.cloud.dependencies.get().toString())
	}
}
