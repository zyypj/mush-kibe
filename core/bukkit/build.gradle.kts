plugins {
    id("muxe.bukkit-plugin")
    id("muxe.server-internals")
}

dependencies {
    api(projects.core.common)

    compileOnly(libs.netty)

    testImplementation(libs.netty)
    testImplementation(testFixtures(projects.core.common))
}
