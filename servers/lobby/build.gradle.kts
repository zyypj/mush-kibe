plugins {
    id("muxe.bukkit-plugin")
}

dependencies {
    compileOnly(projects.core.bukkit)

    testImplementation(projects.core.bukkit)
}
