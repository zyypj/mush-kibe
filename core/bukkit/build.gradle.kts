plugins {
    id("muxe.bukkit-plugin")
}

dependencies {
    api(projects.core.common)

    compileOnly(libs.netty)

    testImplementation(libs.netty)
    testImplementation(testFixtures(projects.core.common))
}
