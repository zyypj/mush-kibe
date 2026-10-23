plugins {
    id("muxe.server-plugin")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("spigot-api").get())
    testImplementation(libs.findLibrary("spigot-api").get())
}

tasks.shadowJar {
    Relocations.bukkitOnly.forEach { pattern ->
        relocate(pattern, "${Relocations.TARGET}.$pattern")
    }
}

tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version.toString(),
    )
    inputs.properties(pluginProperties)
    filesMatching("plugin.yml") {
        expand(pluginProperties)
    }
}
