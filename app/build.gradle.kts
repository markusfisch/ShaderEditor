plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.jetbrains.kotlin.android)
}

android {
	namespace = "de.markusfisch.android.shadereditor"

	compileSdk = 34

	defaultConfig {
		minSdk = 21
		targetSdk = compileSdk

		versionCode = 85
		versionName = "2.34.3"

		vectorDrawables {
			useSupportLibrary = true
		}
	}

	signingConfigs {
		create("release") {
			keyAlias = System.getenv("ANDROID_KEY_ALIAS")
			keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
			storePassword = System.getenv("ANDROID_STORE_PASSWORD")
			storeFile = System.getenv("ANDROID_KEYFILE")?.let { file(it) }
		}
	}

	buildTypes {
		debug {
			applicationIdSuffix = ".debug"
		}

		release {
			isMinifyEnabled = true
			isShrinkResources = true
			signingConfig = signingConfigs["release"]
		}
	}

	buildFeatures {
		buildConfig = true
		viewBinding = true
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}
}

dependencies {
	implementation(libs.androidx.appcompat)
	implementation(libs.material)
	implementation(libs.androidx.preference)

	implementation(libs.androidx.camera.core)
	implementation(libs.androidx.camera.camera2)
	implementation(libs.androidx.camera.lifecycle)
	implementation(libs.androidx.core.ktx)
}