@file:Suppress("UnstableApiUsage")

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.fabricmc.loom.api.mappings.layered.spec.FileSpec
import net.fabricmc.loom.api.mappings.layered.spec.LayeredMappingSpecBuilder
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import java.io.File

fun LayeredMappingSpecBuilder.compatMappings(
    compatSources: List<String>,
    rootDir: File,
) {
    compatSources.forEach { name ->
        val remapsFile = rootDir.resolve("compat/${name}.tiny")
        if (remapsFile.exists()) {
            mappings(remapsFile) {
                mergeNamespace("official")
            }
        }
    }
    compatSources.forEach { name ->
        val renamesFile = rootDir.resolve("compat/${name}.rename.tiny")
        if (renamesFile.exists()) {
            addLayer(PostProcessingMappingSpec(FileSpec.create(renamesFile), "official"))
        }
    }
}

fun addMixinsToFabricModJson(resourcesDir: File) {
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

fun setupCompatSourcePaths(compatSources: List<String>, rootProject: org.gradle.api.Project, sourceSets: SourceSetContainer) {
    sourceSets["main"].java.srcDir(rootProject.file("common/src/main/java"))
    sourceSets["main"].resources.srcDir(rootProject.file("common/src/main/resources"))

    compatSources.forEach { name ->
        sourceSets["main"].java.srcDir(rootProject.file("compat/${name}"))
        sourceSets["main"].resources.srcDir(rootProject.file("compat/${name}_res"))
    }

    sourceSets["test"].java.srcDir(rootProject.file("common/src/test/java"))
    sourceSets["test"].resources.srcDir(rootProject.file("common/src/test/resources"))
}
