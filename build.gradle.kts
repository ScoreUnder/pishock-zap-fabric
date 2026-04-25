import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec

plugins {
    base
    java
    id("net.fabricmc.fabric-loom") apply false // version "$loom_version" -- loaded by buildSrc already
    id("net.fabricmc.fabric-loom-remap") apply false // version "$loom_version" -- loaded by buildSrc already
    `maven-publish`
    id("io.freefair.lombok") version "9.2.0" apply false
}

tasks.register<Copy>("collectJars") {
    val jarTasks = subprojects.map { sub ->
        if (sub.tasks.names.contains("remapJar")) sub.tasks.named("remapJar") else sub.tasks.named("jar")
    }
    dependsOn(jarTasks)
    from(jarTasks)
    into(layout.buildDirectory.dir("libs"))
}

tasks.named("build") {
    dependsOn("collectJars")
}

subprojects {
    val loomPlugin = project.property("loom_plugin") as String
    val modVersion = project.property("mod_version") as String
    val mavenGroup = project.property("maven_group") as String
    val minecraftVersion = project.property("minecraft_version") as String
    val minecraftVersionCompat = project.property("minecraft_version_compat") as String
    val fabricLoaderVersionCompat = project.property("fabric_loader_version_compat") as String
    val fabricApiModName = project.property("fabric_api_mod_name") as String
    val fabricVersion = project.property("fabric_version") as String
    val loaderVersion = project.property("loader_version") as String
    val javaLanguageVersion = project.property("java_language_version") as String
    val archivesBaseName = project.property("archives_base_name") as String
    val yarnMappings = project.findProperty("yarn_mappings") as String?
    val parchmentMappings = project.findProperty("parchment_mappings") as String?
    val compatSources = (project.property("compat_sources") as String).split(",")
    val modmenuVersion = project.property("modmenu_version") as String
    val clothConfigVersion = project.property("cloth_config_version") as String
    val junitVersion = project.property("junit_version") as String

    apply(plugin = loomPlugin)
    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    apply(plugin = "maven-publish")
    apply(plugin = "io.freefair.lombok")

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
        val modImpl = if (configurations.names.contains("modImplementation")) "modImplementation" else "implementation"

        add("minecraft", "com.mojang:minecraft:${minecraftVersion}")
        if (loomPlugin.endsWith("remap")) {
            @Suppress("UnstableApiUsage")
            add("mappings", loom.layered {
                if (yarnMappings != null) {
                    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
                } else {
                    officialMojangMappings()
                    if (parchmentMappings != null) {
                        parchment("org.parchmentmc.data:parchment-${minecraftVersion}:${parchmentMappings}@zip")
                    }
                }

                compatSources.forEach { name ->
                    val remapsFile = rootProject.file("compat/${name}.tiny")
                    if (remapsFile.exists()) {
                        mappings(remapsFile) {
                            mergeNamespace("official")
                        }
                    }
                }

                compatSources.forEach { name ->
                    val renamesFile = rootProject.file("compat/${name}.rename.tiny")
                    if (renamesFile.exists()) {
                        addLayer(PostProcessingMappingSpec(FileSpec.create(renamesFile), "official"))
                    }
                }
            })
        }
        add(modImpl, "net.fabricmc:fabric-loader:${loaderVersion}")
        add(modImpl, "net.fabricmc.fabric-api:fabric-api:${fabricVersion}")
        add(modImpl, "com.terraformersmc:modmenu:${modmenuVersion}")
        add(modImpl, "me.shedaniel.cloth:cloth-config-fabric:${clothConfigVersion}")
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
            val resourcesDir = outputs.files.first()
            val jsonFile = File(resourcesDir, "fabric.mod.json")
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = jsonFile.reader().use { gson.fromJson(it, JsonObject::class.java) }

            val mixins = json.getAsJsonArray("mixins")
            var modified = false
            resourcesDir.walkTopDown().forEach { file ->
                if (file.name.endsWith(".compat.client.mixins.json")) {
                    modified = true
                    mixins.add(JsonObject().apply {
                        addProperty("config", file.name)
                        addProperty("environment", "client")
                    })
                }
            }

            if (modified) {
                jsonFile.writeText(gson.toJson(json))
            }
        }
    }

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
        from("LICENSE") {
            rename { "${it}_${archivesBaseName}" }
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

    sourceSets["main"].java.srcDir(rootProject.file("common/src/main/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("common/src/main/resources"))

    compatSources.forEach { name ->
        sourceSets["main"].java.srcDir(rootProject.file("compat/${name}"))
        sourceSets["main"].resources.srcDir(rootProject.file("compat/${name}_res"))
    }

    sourceSets["test"].java.srcDir(rootProject.file("common/src/test/java"))
    sourceSets["test"].resources.srcDir(rootProject.file("common/src/test/resources"))
}
