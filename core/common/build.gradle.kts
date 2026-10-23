plugins {
    id("muxe.library")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.bundles.postgresql)
    implementation(libs.jedis)

    testImplementation(libs.bundles.testcontainers)
}
