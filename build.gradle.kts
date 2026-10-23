plugins {
    id("muxe.network")
}

network {
    server("proxy", projects.core.proxy)
    server("auth", projects.core.bukkit, projects.servers.auth)
    server("lobby", projects.core.bukkit, projects.servers.lobby)
    server("skywars", projects.core.bukkit, projects.minigames.skywars)
    server("bedwars", projects.core.bukkit, projects.minigames.bedwars)
}
