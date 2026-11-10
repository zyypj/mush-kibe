package br.com.muxe.minigame.api.customroom.bukkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CustomRoomServerTest {

    @Test
    void theCatalogReachesTheNetworkLobbyAndTheGameLobby() {
        assertThat(CustomRoomServer.catalogGroups("lobby", "bedwars-lobby")).containsExactly("lobby", "bedwars-lobby");
    }

    @Test
    void aGameWhoseLobbyIsTheNetworkLobbyPublishesOnce() {
        assertThat(CustomRoomServer.catalogGroups("lobby", "lobby")).containsExactly("lobby");
        assertThat(CustomRoomServer.catalogGroups("lobby", "")).containsExactly("lobby");
    }
}
