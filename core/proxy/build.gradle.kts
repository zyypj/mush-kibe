plugins {
    id("muxe.velocity-plugin")
}

dependencies {
    implementation(projects.core.common)

    testImplementation(testFixtures(projects.core.common))
}
