import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("pishock-zap.base")
}

val loom = extensions.getByType<LoomGradleExtensionAPI>()
val yarnMappings = project.findProperty("yarn_mappings") as String?
val parchmentMappings = project.findProperty("parchment_mappings") as String?

dependencies {
    @Suppress("UnstableApiUsage")
    add("mappings", loom.layered {
        if (yarnMappings != null) {
            mappings("net.fabricmc:yarn:${yarnMappings}:v2")
        } else {
            officialMojangMappings()
            parchmentMappings?.let { parchment("org.parchmentmc.data:parchment-${minecraftVersion}:${it}@zip") }
        }
        compatMappings(compatSources, rootProject.projectDir)
    })
    addModDeps("modImplementation", modVersions)
}
