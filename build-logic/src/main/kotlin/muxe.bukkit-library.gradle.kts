plugins {
    id("muxe.library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    compileOnly(libs.findLibrary("spigot-api").get())
    testImplementation(libs.findLibrary("spigot-api").get())
}
