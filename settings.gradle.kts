pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
            name = "spigotmc"
            mavenContent {
                snapshotsOnly()
            }
        }
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "muxe"

include(":core:common")
include(":core:bukkit")
include(":core:proxy")

include(":servers:auth")
include(":servers:lobby")

include(":minigames:engine")
include(":minigames:skywars")
include(":minigames:bedwars")
