plugins {
    id("muxe.velocity-plugin")
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.geoip2)

    testImplementation(testFixtures(projects.core.common))
}
