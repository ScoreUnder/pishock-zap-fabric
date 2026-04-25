import java.util.Properties

plugins {
    java
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
