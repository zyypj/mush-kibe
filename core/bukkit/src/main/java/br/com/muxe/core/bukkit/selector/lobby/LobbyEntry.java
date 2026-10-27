package br.com.muxe.core.bukkit.selector.lobby;

public record LobbyEntry(String serverId, String label, int online, int max, boolean joinable, boolean current) {

    public boolean hasRoom() {
        return joinable && online < max;
    }
}
