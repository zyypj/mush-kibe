package br.com.muxe.lobby.placement;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.npc.placed.NpcLines;
import br.com.muxe.core.bukkit.npc.placed.PlacedNpcs;
import br.com.muxe.core.bukkit.placement.PlacementImport;
import br.com.muxe.core.common.placement.domain.HeldItem;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placement;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.SkinTexture;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class GameNpcImportTest {

    private static final File FILE = new File("placements/import.yml");
    private static final NpcLines DEFAULT_LINES = new NpcLines(new EnumMap<>(Map.of(NpcAction.Type.GAME, List.of("&b&l{name}", "&e{players} jogando"))));

    private final List<NpcPlacement> npcs = PlacementImport.read(YamlConfiguration.loadConfiguration(FILE), "lobby", Logger.getLogger("test")).stream()
        .map(NpcPlacement.class::cast)
        .toList();

    @Test
    void theTwelveGameNpcsStandInARowAtTheExportedSpots() {
        assertThat(FILE).isFile();
        assertThat(npcs).extracting(NpcPlacement::name).containsExactly(
            "HG", "MURDER", "PVP", "BUILD", "TREINO: PONTE", "DUELS", "BED WARS", "SKY WARS", "THE BRIDGE", "PARTY", "HIDE AND SEEK", "CTF");
        assertThat(npcs).extracting(npc -> npc.position().x()).containsExactly(12.5, 10.5, 8.5, 6.5, 4.5, 2.5, 0.5, -1.5, -3.5, -5.5, -7.5, -9.5);
        assertThat(npcs).extracting(npc -> npc.position().z()).containsExactly(1.5, 2.5, 3.5, 3.5, 2.5, 1.5, 1.5, 2.5, 3.5, 3.5, 2.5, 1.5);
        assertThat(npcs).allSatisfy(npc -> {
            assertThat(npc.group()).isEqualTo("lobby");
            assertThat(npc.position().world()).isEqualTo("lobby");
            assertThat(npc.position().y()).isEqualTo(33.375);
            assertThat(npc.position().pitch()).isZero();
        });
        assertThat(npcs.getFirst().position()).isEqualTo(new Position("lobby", 12.5, 33.375, 1.5, 151.875f, 0));
        assertThat(npcs.getLast().position()).isEqualTo(new Position("lobby", -9.5, 33.375, 1.5, -151.875f, 0));
    }

    @Test
    void everyNpcSendsPlayersToItsGameGroup() {
        Map<String, String> groups = new LinkedHashMap<>();
        npcs.forEach(npc -> groups.put(npc.name(), npc.action().target()));

        assertThat(npcs).allSatisfy(npc -> assertThat(npc.action().type()).isEqualTo(NpcAction.Type.GAME));
        assertThat(groups).containsExactly(
            Map.entry("HG", "hg"),
            Map.entry("MURDER", "murder"),
            Map.entry("PVP", "pvp"),
            Map.entry("BUILD", "buildbattle"),
            Map.entry("TREINO: PONTE", "treinoponte"),
            Map.entry("DUELS", "duels"),
            Map.entry("BED WARS", "bedwars"),
            Map.entry("SKY WARS", "skywars"),
            Map.entry("THE BRIDGE", "thebridge"),
            Map.entry("PARTY", "party"),
            Map.entry("HIDE AND SEEK", "hideandseek"),
            Map.entry("CTF", "ctf")
        );
    }

    @Test
    void theLinesShowTheNameAndTheLiveCountExceptForTheTwoSpecialNpcs() {
        Map<String, List<String>> shown = new LinkedHashMap<>();
        npcs.forEach(npc -> shown.put(npc.id(), DEFAULT_LINES.render(npc, 7)));

        assertThat(shown.get("hg")).containsExactly("&b&lHG", "&e7 jogando");
        assertThat(shown.get("bedwars")).containsExactly("&b&lBED WARS", "&e7 jogando");
        assertThat(shown.get("duels")).containsExactly("&d&lNOVIDADE!", "&b&lDUELS", "&e7 jogando");
        assertThat(shown.get("treinoponte")).containsExactly("&b&lTREINO: PONTE", "&eClique aqui!");
        assertThat(npcs).filteredOn(NpcPlacement::hasOwnLines).extracting(NpcPlacement::id).containsExactly("treinoponte", "duels");
    }

    @Test
    void everyNpcHoldsAOneEightItemAndWearsASignedMinecraftSkin() {
        Map<String, HeldItem> hands = new LinkedHashMap<>();
        npcs.forEach(npc -> hands.put(npc.id(), npc.hand().orElseThrow()));

        assertThat(hands.get("hg")).isEqualTo(new HeldItem("MUSHROOM_SOUP", 0));
        assertThat(hands.get("ctf")).isEqualTo(new HeldItem("WOOL", 14));
        assertThat(hands.get("duels")).isEqualTo(new HeldItem("DIAMOND_SWORD", 0));
        assertThat(hands.values()).allSatisfy(item -> assertThat(PlacedNpcs.hand(item)).isPresent());
        assertThat(npcs).allSatisfy(npc -> {
            SkinTexture skin = npc.skin().orElseThrow();
            assertThat(new String(Base64.getDecoder().decode(skin.value()), StandardCharsets.UTF_8)).contains("http://textures.minecraft.net/texture/");
            assertThat(skin.signature()).isNotBlank();
        });
    }

    @Test
    void everyEntryOfTheFileBecomesAnNpc() {
        List<Placement> all = PlacementImport.read(YamlConfiguration.loadConfiguration(FILE), "lobby", Logger.getLogger("test"));

        assertThat(all).hasSize(12).allMatch(NpcPlacement.class::isInstance);
    }
}
