plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

group = "br.com.muxe"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial,-options"))
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(platform(libs.findLibrary("testcontainers-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj").get())
    testRuntimeOnly(libs.findLibrary("junit-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

afterEvaluate {
    val module = path
    val allowed = ModuleRules.allowedFor(module)
        ?: throw GradleException("Module $module has no dependency rule. Register it in build-logic/src/main/kotlin/ModuleRules.kt.")
    val forbidden = configurations
        .flatMap { it.dependencies.withType<ProjectDependency>() }
        .map { it.path }
        .filter { target -> target != module && allowed.none { group -> ModuleRules.belongsTo(target, group) } }
        .distinct()
    if (forbidden.isNotEmpty()) {
        throw GradleException("Module $module cannot depend on ${forbidden.joinToString()}. Allowed: ${allowed.joinToString()}. See docs/development/README.md.")
    }
}
