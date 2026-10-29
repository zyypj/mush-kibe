package br.com.muxe.core.common.testing;

import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.Medal;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.Tag;
import br.com.muxe.core.common.permission.domain.Track;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PermissionFixtures {

    public static final Instant SEEDED_AT = Instant.parse("2026-10-28T12:00:00Z");
    public static final List<String> STAFF_PERMISSIONS = List.of("muxe.account.others", "muxe.account.staff");
    public static final List<String> RANKS = List.of("membro", "vip", "mvp", "pro", "ultra", "partner", "partner+", "mod", "mod+", "admin");
    public static final List<Medal> MEDALS = List.of(
        new Medal("toxico", "Tóxico", "☣", "&a", 10),
        new Medal("yin-yang", "Yin Yang", "☯", "&f", 20),
        new Medal("musico", "Músico", "♫", "&5", 30),
        new Medal("flash", "Flash", "⚡", "&6", 40),
        new Medal("paz-e-amor", "Paz e Amor", "✌", "&c", 50),
        new Medal("amor", "Amor", "❤", "&4", 60),
        new Medal("cafe", "Café", "☕", "&e", 70),
        new Medal("carinha", "Carinha", "ツ", "&6", 80),
        new Medal("halloween", "Halloween", "✝", "&4", 90),
        new Medal("natal", "Natal", "✽", "&b", 100),
        new Medal("cacador-de-bruxas", "Caçador de Bruxas", "∅", "&2", 110),
        new Medal("casa-de-gengibre", "Casa de Gengibre", "۩", "&6", 120),
        new Medal("alerta", "Alerta", "⚠", "&e", 130),
        new Medal("atomo", "Átomo", "⚛", "&b", 140),
        new Medal("anel", "Anel", "℧", "&6", 150),
        new Medal("boneco-de-neve", "Boneco de Neve", "☃", "&f", 160),
        new Medal("carnaval", "Carnaval", "✴", "&6", 170),
        new Medal("feiticeiro-aprendiz", "Feiticeiro Aprendiz", "⊰⊱", "&a", 180),
        new Medal("biscoito-de-gengibre", "Biscoito de Gengibre", "۞", "&6", 190),
        new Medal("liga-prata", "Liga Prata (Bed Wars Champions)", "✶", "&f", 200),
        new Medal("fogo", "Fogo", "火", "&c", 210),
        new Medal("agua", "Água", "水", "&b", 220),
        new Medal("ar", "Ar", "気", "&f", 230),
        new Medal("estrela", "Estrela", "✯", "&e", 240),
        new Medal("sortudo", "Sortudo", "☘", "&2", 250),
        new Medal("carta", "Carta", "回", "&f", 260),
        new Medal("modo-aviao", "Modo Avião", "✈", "&b", 270),
        new Medal("celular", "Celular", "✆", "&a", 280),
        new Medal("flor", "Flor", "✿", "&d", 290),
        new Medal("briga-de-espadas", "Briga de Espadas", "⚔", "&7", 300),
        new Medal("imperial", "Imperial", "⚜", "&6", 310)
    );
    public static final String LOBBY_FLY = "muxe.lobby.fly";
    public static final Map<String, List<String>> MEDAL_GRANTS = Map.of(
        "vip", List.of("carinha", "toxico", "yin-yang"),
        "ultra", List.of("musico", "flash", "paz-e-amor", "amor", "cafe")
    );
    public static final List<Tag> TAGS = List.of(
        new Tag("membro", "Membro", "&7", 0, Optional.of(""), false),
        new Tag("vip", "VIP", "&a", 10, Optional.empty(), false),
        new Tag("mvp", "MVP", "&9", 20, Optional.empty(), false),
        new Tag("pro", "Pro", "&6", 30, Optional.empty(), false),
        new Tag("ultra", "Ultra", "&d", 40, Optional.empty(), false),
        new Tag("2021", "2021", "&b", 50, Optional.empty(), true),
        new Tag("2022", "2022", "&b", 51, Optional.empty(), true),
        new Tag("2023", "2023", "&b", 52, Optional.empty(), true),
        new Tag("2024", "2024", "&b", 53, Optional.empty(), true),
        new Tag("2025", "2025", "&b", 54, Optional.empty(), true),
        new Tag("2026", "2026", "&b", 55, Optional.empty(), true),
        new Tag("beta", "Beta", "&1", 60, Optional.empty(), true),
        new Tag("enderlore", "Enderlore", "&5", 70, Optional.empty(), true),
        new Tag("carnaval", "Carnaval", "&6", 80, Optional.empty(), true),
        new Tag("natal", "Natal", "&c", 90, Optional.empty(), true),
        new Tag("ferias", "Ferias", "&a", 100, Optional.empty(), true),
        new Tag("ferias-2", "Ferias", "&2", 101, Optional.empty(), true),
        new Tag("partner", "Partner", "&b", 110, Optional.empty(), false),
        new Tag("partner+", "Partner+", "&1", 120, Optional.empty(), false),
        new Tag("mod", "Mod", "&5", 130, Optional.empty(), false),
        new Tag("mod+", "Mod+", "&5", 140, Optional.empty(), false),
        new Tag("admin", "Admin", "&c", 150, Optional.empty(), false)
    );
    public static final PermissionData DATA = data();

    private PermissionFixtures() {
    }

    private static PermissionData data() {
        List<Group> groups = new ArrayList<>();
        for (int index = 0; index < RANKS.size(); index++) {
            String id = RANKS.get(index);
            Tag tag = TAGS.stream().filter(candidate -> candidate.id().equals(id)).findFirst().orElseThrow();
            List<PermissionNode> nodes = new ArrayList<>();
            if (index > 0) {
                nodes.add(PermissionNode.group(RANKS.get(index - 1), Optional.empty(), SEEDED_AT));
            }
            MEDAL_GRANTS.getOrDefault(id, List.of()).forEach(medal -> nodes.add(PermissionNode.permanent(Medal.PERMISSION_PREFIX + medal, true, SEEDED_AT)));
            if (id.equals("vip")) {
                nodes.add(PermissionNode.permanent(LOBBY_FLY, true, SEEDED_AT));
            }
            if (id.equals("mod")) {
                STAFF_PERMISSIONS.forEach(permission -> nodes.add(PermissionNode.permanent(permission, true, SEEDED_AT)));
            }
            if (id.equals("admin")) {
                nodes.add(PermissionNode.permanent(PermissionNode.WILDCARD, true, SEEDED_AT));
            }
            groups.add(new Group(id, tag.name(), tag.weight(), Optional.of(id), nodes));
        }
        List<Track> tracks = List.of(
            new Track("doacao", List.of("membro", "vip", "mvp", "pro", "ultra")),
            new Track("staff", List.of("mod", "mod+", "admin"))
        );
        return PermissionData.of(groups, TAGS, tracks, MEDALS);
    }
}
