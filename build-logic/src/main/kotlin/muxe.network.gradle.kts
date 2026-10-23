plugins {
    base
}

tasks.register("assembleServers") {
    group = "muxe"
    description = "Assembles the plugin jars of every server into build/servers."
}

tasks.assemble {
    dependsOn("assembleServers")
}

extensions.create<NetworkExtension>("network", project)
