plugins {
    id("muxe.bukkit-plugin")
}

dependencies {
    api(projects.core.common)

    compileOnly(libs.netty)
}
