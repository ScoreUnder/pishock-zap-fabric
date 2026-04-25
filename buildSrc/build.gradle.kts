import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    java
    `kotlin-dsl`
}

val rootProps = Properties()
file("../gradle.properties").inputStream().use { stream ->
    rootProps.load(stream)
}

repositories {
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:fabric-loom:${rootProps["loom_version"]}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set((rootProps["java_language_version"] as String).toInt())
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(rootProps["java_language_version"] as String))
}
