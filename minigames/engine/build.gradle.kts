plugins {
    id("muxe.bukkit-library")
    id("muxe.server-internals")
}

dependencies {
    compileOnly(projects.core.bukkit)

    testImplementation(projects.core.bukkit)
}
