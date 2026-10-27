package br.com.muxe.core.common.placement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlacementTest {

    private static final Position SPAWN = new Position("lobby", 7.5, 33, -9.5, 180, 0);

    @Test
    void npcActionsAreReadFromOneWord() {
        assertThat(NpcAction.parse("loja")).isEqualTo(NpcAction.SHOP);
        assertThat(NpcAction.parse("NENHUMA")).isEqualTo(NpcAction.NONE);
        assertThat(NpcAction.parse("SkyWars")).isEqualTo(NpcAction.game("skywars"));
        assertThat(NpcAction.game("bedwars").describe()).isEqualTo("bedwars");
        assertThat(NpcAction.SHOP.describe()).isEqualTo("loja");
    }

    @Test
    void onlyGameActionsKeepATarget() {
        assertThat(new NpcAction(NpcAction.Type.SHOP, "ignored").target()).isEmpty();
        assertThatThrownBy(() -> new NpcAction(NpcAction.Type.GAME, " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void idsAreShortLowercaseWords() {
        assertThat(PlacementIds.isValid("caixa-1")).isTrue();
        assertThat(PlacementIds.isValid("Caixa")).isFalse();
        assertThat(PlacementIds.isValid("a".repeat(33))).isFalse();
        assertThat(PlacementIds.isValid("shop:seller")).isFalse();
        assertThatThrownBy(() -> new RegisterPlacement("lobby", "Caixa", SPAWN, Optional.empty())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RegisterPlacement(" ", "caixa", SPAWN, Optional.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hologramLinesAreNumberedFromOne() {
        HologramPlacement hologram = new HologramPlacement("lobby", "boas-vindas", SPAWN, List.of("&bMuxe"));

        HologramPlacement changed = hologram.adding("&eBem-vindo").replacing(1, "&b&lMUXE").adding("&7tchau").without(3);

        assertThat(changed.lines()).containsExactly("&b&lMUXE", "&eBem-vindo");
        assertThat(changed.hasLine(0)).isFalse();
        assertThat(changed.hasLine(2)).isTrue();
        assertThatThrownBy(() -> changed.without(3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aHologramKeepsAtLeastOneLine() {
        HologramPlacement hologram = new HologramPlacement("lobby", "um", SPAWN, List.of("&bMuxe"));

        assertThatThrownBy(() -> hologram.without(1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void npcsChangeOnePropertyAtATime() {
        NpcPlacement npc = new NpcPlacement("lobby", "skywars", SPAWN, "SkyWars", NpcAction.game("skywars"), "Notch", Optional.empty());
        Position elsewhere = new Position("lobby", 0, 40, 0, 90, 0);

        NpcPlacement changed = npc.movedTo(elsewhere).named("Sky").doing(NpcAction.SHOP).wearing("jeb_", Optional.of(new SkinTexture("v", "s")));

        assertThat(changed).isEqualTo(new NpcPlacement("lobby", "skywars", elsewhere, "Sky", NpcAction.SHOP, "jeb_", Optional.of(new SkinTexture("v", "s"))));
    }

    @Test
    void placementsAreFoundById() {
        RegisterPlacement register = new RegisterPlacement("lobby", "caixa", SPAWN, Optional.empty());
        Placements placements = new Placements(List.of(), List.of(), List.of(register), List.of());

        assertThat(placements.register("caixa")).contains(register);
        assertThat(placements.npc("caixa")).isEmpty();
        assertThat(register.withSeller(Optional.of(SPAWN)).seller()).contains(SPAWN);
    }

    @Test
    void socialHeadsAreFoundByIdAndMove() {
        SocialHeadPlacement head = new SocialHeadPlacement("lobby", "redes", SPAWN);
        Position elsewhere = new Position("lobby", 4, 34, -10, 90, 0);
        Placements placements = new Placements(List.of(), List.of(), List.of(), List.of(head));

        assertThat(placements.socialHead("redes")).contains(head);
        assertThat(placements.register("redes")).isEmpty();
        assertThat(head.movedTo(elsewhere).position()).isEqualTo(elsewhere);
        assertThat(head.kind()).isEqualTo(PlacementKind.SOCIAL_HEAD);
        assertThatThrownBy(() -> new SocialHeadPlacement("lobby", "Redes!", SPAWN)).isInstanceOf(IllegalArgumentException.class);
    }
}
