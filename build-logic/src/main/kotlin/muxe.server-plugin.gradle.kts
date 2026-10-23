plugins {
    id("muxe.library")
    id("com.gradleup.shadow")
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.shadowJar {
    archiveFileName = "${PluginJars.nameFor(project.path)}.jar"
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    dependencies {
        Relocations.annotationOnly.forEach { module ->
            exclude(dependency(module))
        }
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/*/module-info.class", "module-info.class")
    Relocations.shared.forEach { pattern ->
        relocate(pattern, "${Relocations.TARGET}.$pattern")
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

configurations.consumable("serverPlugin") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(SERVER_PLUGIN_USAGE))
    }
    outgoing.artifact(tasks.shadowJar)
}
