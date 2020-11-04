plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        applicationId = "couscous.formulaone"
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode(1)
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    sourceSets.all {
        java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.10")
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("com.google.android.material:material:1.2.1")
}