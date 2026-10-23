plugins {
    id("muxe.bukkit-plugin")
}

dependencies {
    compileOnly(projects.core.bukkit)
    implementation(projects.minigames.engine)

    testImplementation(projects.core.bukkit)
}
