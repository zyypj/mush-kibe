plugins {
    id("muxe.bukkit-library")
}

dependencies {
    compileOnly(projects.core.bukkit)

    testImplementation(projects.core.bukkit)
}
