plugins {
	alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}

subprojects {
	tasks.withType<JavaCompile>().configureEach {
		options.compilerArgs.add("-Xlint:unchecked")
	}
}

tasks.register<Delete>("clean") {
	delete(rootProject.layout.buildDirectory)
}