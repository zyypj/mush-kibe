plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")
val pandaSpigotBuild = libs.findVersion("pandaspigot").get().requiredVersion
val pandaSpigotDirectory = layout.dir(provider { File(gradle.gradleUserHomeDir, "caches/muxe/pandaspigot/$pandaSpigotBuild") })

val patchPandaSpigot = tasks.register<PatchPandaSpigot>("patchPandaSpigot") {
    group = "muxe"
    description = "Downloads PandaSpigot $pandaSpigotBuild and patches it to compile against the 1.8.8 server internals."
    build = pandaSpigotBuild
    sha256 = libs.findVersion("pandaspigot-sha256").get().requiredVersion
    workDirectory = pandaSpigotDirectory
    patchedJar = pandaSpigotDirectory.map { it.file("cache/patched_1.8.8.jar") }
    ready = layout.buildDirectory.file("pandaspigot/ready.txt")
    launcher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

dependencies {
    compileOnly(files(patchPandaSpigot.flatMap { it.patchedJar }).builtBy(patchPandaSpigot))
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("-Xlint:-classfile")
}
