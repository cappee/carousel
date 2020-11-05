import java.io.FileInputStream
import org.jetbrains.kotlin.konan.properties.Properties

val properties = Properties()
val propsFile = file("github.properties")
if (propsFile.exists()) {
    properties.load(FileInputStream(file("github.properties")))
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    id("maven-publish")
    id("com.github.dcendents.android-maven")
}

android {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.2"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode(1)
        versionName = "1.2.3"
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
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("me.relex:circleindicator:2.1.4")
}

if (propsFile.exists()) {
    publishing {
        repositories {
            maven {
                name = "GithubPackages"
                url = uri("https://maven.pkg.github.com/cappee/carousel")
                credentials {
                    username = properties["gpr.user"] as String
                    password = properties["gpr.key"] as String
                }
            }
        }
        publications {
            register("gpr", MavenPublication::class) {
                groupId = "dev.cappee"
                artifactId = "carousel-android"
                version = "1.2.3"
                artifact("$buildDir/outputs/aar/${project.name}-release.aar")
                pom {
                    withXml {
                        val dependencies = asNode().appendNode("dependencies")
                        configurations.implementation.get().dependencies.forEach {
                            if (it.group != null && "unspecified" != it.name && it.version != null) {
                                val dependencyNode = dependencies.appendNode("dependency")
                                dependencyNode.appendNode("groupId", it.group)
                                dependencyNode.appendNode("artifactId", it.name)
                                dependencyNode.appendNode("version", it.version)
                            }
                        }
                    }
                }
            }
        }
    }
}
