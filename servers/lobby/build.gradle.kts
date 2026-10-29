plugins {
    id("muxe.bukkit-plugin")
}

dependencies {
    compileOnly(projects.core.bukkit)

    testImplementation(projects.core.bukkit)
    testImplementation(testFixtures(projects.core.common))
    testImplementation(libs.bundles.testcontainers)
}
