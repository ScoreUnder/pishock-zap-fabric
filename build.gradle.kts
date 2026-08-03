plugins {
    base
}

tasks.register<Copy>("collectJars") {
    description = "Copy JARs from subprojects to main build directory"
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
