plugins {
    base
    java
    `maven-publish`
    id("io.freefair.lombok")
}

val modVersion = project.property("mod_version") as String
val mavenGroup = project.property("maven_group") as String
val minecraftVersionCompat = project.property("minecraft_version_compat") as String
val fabricLoaderVersionCompat = project.property("fabric_loader_version_compat") as String
val fabricApiModName = project.property("fabric_api_mod_name") as String
val javaLanguageVersion = project.property("java_language_version") as String
val archivesBaseName = project.property("archives_base_name") as String
val junitVersion = project.property("junit_version") as String

version = "${modVersion}+${minecraftVersion}"
group = mavenGroup

base {
    archivesName.set(archivesBaseName)
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.shedaniel.me/") }
    maven { name = "ParchmentMC"; url = uri("https://maven.parchmentmc.org/") }
}

dependencies {
    add("minecraft", "com.mojang:minecraft:${minecraftVersion}")
    implementation("com.fazecast:jSerialComm:[2.0.0,3.0.0)")
    add("include", "com.fazecast:jSerialComm:[2.0.0,3.0.0)")
    testImplementation("com.tngtech.archunit:archunit:1.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.guava:guava:21.0") // Transitively pulled in by minecraft
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
        "version" to version,
        "minecraft_version_compat" to minecraftVersionCompat,
        "java_version" to javaLanguageVersion,
        "fabric_loader_version_compat" to fabricLoaderVersionCompat,
        "fabric_api_mod_name" to fabricApiModName,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "*.mixins.json")) {
        expand(props)
    }

    doLast {
        addMixinsToFabricModJson(destinationDir)
    }
}

setupCompatSourcePaths(compatSources, rootProject, sourceSets)

val projJavaVersion = javaLanguageVersion.toInt()
tasks.withType<JavaCompile>().configureEach {
    options.release.set(projJavaVersion)
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaLanguageVersion)
    targetCompatibility = JavaVersion.toVersion(javaLanguageVersion)
}

tasks.named<JavaCompile>("compileTestJava") {
    val baseJavaVersion = (rootProject.property("java_language_version") as String).toInt()
    options.release.set(maxOf(baseJavaVersion, projJavaVersion))
}

tasks.named<Jar>("jar") {
    var suffix = archivesBaseName
    from("LICENSE") {
        rename { "${it}_${suffix}" }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
