plugins {
    id("muxe.server-plugin")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("velocity-api").get())
    testImplementation(libs.findLibrary("velocity-api").get())
}

tasks.shadowJar {
    dependencies {
        Relocations.providedByVelocity.forEach { module ->
            exclude(dependency(module))
        }
    }
}

tasks.processResources {
    val pluginProperties = mapOf(
        "version" to project.version.toString(),
    )
    inputs.properties(pluginProperties)
    filesMatching("velocity-plugin.json") {
        expand(pluginProperties)
    }
}
