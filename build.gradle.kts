plugins {
	alias(libs.plugins.android.application) apply false
}

subprojects {
	tasks.withType<JavaCompile>().configureEach {
		options.compilerArgs.add("-Xlint:unchecked")
	}
}

tasks.register<Delete>("clean") {
	delete(rootProject.layout.buildDirectory)
}