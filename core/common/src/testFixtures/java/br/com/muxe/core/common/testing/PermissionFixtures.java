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
    public static final List<String> RANKS = List.of("membro", "vip", "mvp", "pro", "ultra", "ultra+", "partner", "partner+", "builder", "helper", "trial", "mod", "mod+", "admin");
    public static final List<Medal> MEDALS = List.of(
        new Medal("toxico", "Tóxico", "☣", "&a", 10),
        new Medal("yin-yang", "Yin Yang", "☯", "&f", 20),
        new Medal("musico", "Músico", "♫", "&5", 30),
        new Medal("flash", "Flash", "&l⚡", "&6", 40),
        new Medal("paz-e-amor", "Paz e Amor", "✌", "&c", 50),
        new Medal("amor", "Amor", "❤", "&4", 60),
        new Medal("cafe", "Café", "☕", "&e", 70),
        new Medal("carinha", "Carinha", "&lツ", "&6", 80),
        new Medal("halloween", "Halloween", "&l✝", "&4", 90),
        new Medal("natal", "Natal", "✽", "&b", 100),
        new Medal("cacador-de-bruxas", "Caçador de Bruxas", "∅", "&2", 110),
        new Medal("casa-de-gengibre", "Casa de Gengibre", "&l۩", "&6", 120),
        new Medal("alerta", "Alerta", "&l⚠", "&e", 130),
        new Medal("atomo", "Átomo", "⚛", "&b", 140),
        new Medal("anel", "Anel", "℧", "&6", 150),
        new Medal("boneco-de-neve", "Boneco de Neve", "☃", "&f", 160),
        new Medal("carnaval", "Carnaval", "&l✷", "&6", 170),
        new Medal("feiticeiro-aprendiz", "Feiticeiro Aprendiz", "&l⊰⊱", "&a", 180),
        new Medal("biscoito-de-gengibre", "Biscoito de Gengibre", "&l۞", "&6", 190),
        new Medal("liga-prata", "Liga Prata (Bed Wars Champions)", "✶", "&f", 200),
        new Medal("fogo", "Fogo", "火", "&c", 210),
        new Medal("agua", "Água", "水", "&b", 220),
        new Medal("ar", "Ar", "氣", "&f", 230),
        new Medal("estrela", "Estrela", "✯", "&e", 240),
        new Medal("sortudo", "Sortudo", "&l☘", "&2", 250),
        new Medal("carta", "Carta", "回", "&f", 260),
        new Medal("modo-aviao", "Modo Avião", "✈", "&b", 270),
        new Medal("celular", "Celular", "✆", "&a", 280),
        new Medal("flor", "Flor", "✿", "&d", 290),
        new Medal("briga-de-espadas", "Briga de Espadas", "⚔", "&7", 300),
        new Medal("imperial", "Imperial", "⚜", "&6", 310),
        new Medal("quartel-quell-1", "Quartel Quell 1", "✪", "&6", 320),
        new Medal("quartel-quell-2", "Quartel Quell 2", "➣", "&6", 330),
        new Medal("quartel-quell-3", "Quartel Quell 3", "✵", "&6", 340),
        new Medal("general-season-3", "General Season 3", "❂", "&4", 350),
        new Medal("master", "Master", "❂", "&4", 360),
        new Medal("errado", "Errado", "&l✖", "&c", 370),
        new Medal("cxc", "Cxc", "&l✠", "&c", 380),
        new Medal("nitro", "Nitro", "&l✦", "&0", 390),
        new Medal("bgs", "BGS", "&l➤", "&3", 400),
        new Medal("top", "Top", "❁", "&b", 410),
        new Medal("arzim", "Arzim", "✵", "&d", 420),
        new Medal("aluno", "Aluno", "✏", "&5", 430),
        new Medal("muxe", "Muxe", "&lM", "&b", 440),
        new Medal("wg", "WG", "&lW", "&b", 450),
        new Medal("general", "General", "✫", "&4", 460),
        new Medal("grammy", "Grammy", "♪", "&6", 470),
        new Medal("promotor-de-eventos", "Promotor de Eventos", "❃", "&5", 480),
        new Medal("staff", "Staff", "✷", "&1", 490),
        new Medal("helper", "Helper", "✤", "&9", 500),
        new Medal("mod", "Mod", "✥", "&b", 510),
        new Medal("ajudante-de-papai-noel", "Ajudante de Papai Noel", "❀", "&6", 520),
        new Medal("mega-muxe", "Mega Muxe", "✹", "&4", 530),
        new Medal("correto", "Correto", "&l✓", "&a", 540),
        new Medal("aniversario-do-muxe-2022", "Aniversario do Muxe 2022", "❣", "&c", 550),
        new Medal("yt", "YT", "&l✩", "&b", 560),
        new Medal("liga-diamante", "Liga Diamante (Bed Wars Champions)", "✫", "&d", 570),
        new Medal("partner", "Partner", "&l✧", "&b", 580),
        new Medal("nivel-100", "Nivel 100", "&l✩", "&6", 590),
        new Medal("aniversario-do-muxe-2023", "Aniversario do Muxe 2023", "✺", "&e", 600),
        new Medal("pix", "Pix", "❖", "&3", 610),
        new Medal("feiticeiro", "Feiticeiro", "✲", "&c", 620),
        new Medal("estacio", "Estacio", "✏", "&b", 630),
        new Medal("ruby", "Ruby", "✦", "&c", 640),
        new Medal("grana", "Grana", "&l$", "&a", 650),
        new Medal("catapato", "CataPato", "✕", "&e", 660),
        new Medal("floco-de-neve", "Floco de Neve", "❅", "&f", 670),
        new Medal("show-de-fogos-2024", "Show de Fogos 2024", "✺", "&c", 680),
        new Medal("mod+", "Mod+", "❤", "&5", 690),
        new Medal("muxe-league", "Muxe League", "❂", "&6", 700),
        new Medal("clan-clash", "Clan Clash", "✽", "&6", 710),
        new Medal("feiticeiro-amador", "Feiticeiro Amador", "&l⊰&e✰&a&l⊱", "&a", 720),
        new Medal("feiticeiro-mestre", "Feiticeiro Mestre", "&l⊰&e✰&d&l⊱", "&d", 730),
        new Medal("show-de-fogos-2025", "Show de Fogos 2025", "✺", "&a", 740),
        new Medal("top-cyan", "Top Cyan", "❁", "&3", 750),
        new Medal("top-dark-blue", "Top Dark Blue", "❁", "&1", 760),
        new Medal("top-red", "Top Red", "❁", "&4", 770),
        new Medal("gamescom", "Gamescom", "۞", "&f", 780),
        new Medal("desenhista", "Desenhista", "✎", "&6", 790),
        new Medal("ajudante-da-nasco", "Ajudante da Nascó", "⚝", "&6", 800),
        new Medal("clan-clash-2", "Clan Clash 2", "✟", "&6", 810),
        new Medal("show-de-fogos-2026", "Show de Fogos 2026", "✺", "&6", 820),
        new Medal("liga-mestre", "Liga Mestre (Bed Wars Champions)", "✪", "&4", 830),
        new Medal("liga-platina", "Liga Platina (Bed Wars Champions)", "✹", "&3", 840),
        new Medal("liga-ouro", "Liga Ouro (Bed Wars Champions)", "&l✷", "&6", 850),
        new Medal("satturnni", "Satturnni", "➊", "&6", 860),
        new Medal("kindome", "Kindome", "&lK", "&5", 870)
    );
    public static final String LOBBY_FLY = "muxe.lobby.fly";
    public static final String AFK_BYPASS = "muxe.afk.bypass";
    public static final String PARTY_OPEN = "muxe.party.open";
    public static final Map<String, List<String>> MEDAL_GRANTS = Map.of(
        "vip", List.of("carinha", "toxico", "yin-yang"),
        "ultra", List.of("musico", "flash", "paz-e-amor", "amor", "cafe")
    );
    public static final List<Tag> TAGS = List.of(
        new Tag("membro", "Membro", "&7", 0, Optional.of(""), false),
        new Tag("blade", "Blade", "&e", 4, Optional.empty(), true),
        new Tag("master", "Master", "&6", 6, Optional.empty(), true),
        new Tag("vip", "VIP", "&a", 10, Optional.empty(), false),
        new Tag("mvp", "MVP", "&9", 20, Optional.empty(), false),
        new Tag("pro", "Pro", "&6", 30, Optional.empty(), false),
        new Tag("ultra", "Ultra", "&d", 40, Optional.empty(), false),
        new Tag("enderlore", "Enderlore", "&5", 41, Optional.empty(), true),
        new Tag("ferias", "Ferias", "&a", 42, Optional.empty(), true),
        new Tag("ferias-2", "Ferias", "&2", 43, Optional.empty(), true),
        new Tag("ferias-3", "Ferias", "&9", 44, Optional.empty(), true),
        new Tag("carnaval", "Carnaval", "&6", 45, Optional.empty(), true),
        new Tag("natal", "Natal", "&c", 46, Optional.empty(), true),
        new Tag("ultra+", "Ultra&6+", "&5", 50, Optional.of("&5&lULTRA&6+ "), false),
        new Tag("2017", "2017", "&b", 51, Optional.empty(), true),
        new Tag("2018", "2018", "&b", 52, Optional.empty(), true),
        new Tag("2019", "2019", "&b", 53, Optional.empty(), true),
        new Tag("2020", "2020", "&b", 54, Optional.empty(), true),
        new Tag("2021", "2021", "&b", 55, Optional.empty(), true),
        new Tag("2022", "2022", "&b", 56, Optional.empty(), true),
        new Tag("2023", "2023", "&b", 57, Optional.empty(), true),
        new Tag("2024", "2024", "&b", 58, Optional.empty(), true),
        new Tag("2025", "2025", "&b", 59, Optional.empty(), true),
        new Tag("2026", "2026", "&b", 60, Optional.empty(), true),
        new Tag("nitro", "Nitro", "&d", 61, Optional.empty(), true),
        new Tag("beta", "Beta", "&1", 62, Optional.empty(), true),
        new Tag("champion", "Champion", "&6", 63, Optional.empty(), true),
        new Tag("stream", "Stream", "&b", 64, Optional.empty(), true),
        new Tag("yt", "YT", "&b", 65, Optional.empty(), true),
        new Tag("partner", "Partner", "&b", 110, Optional.empty(), false),
        new Tag("partner+", "Partner+", "&3", 120, Optional.empty(), false),
        new Tag("builder", "Builder", "&2", 122, Optional.empty(), false),
        new Tag("helper", "Helper", "&9", 124, Optional.empty(), false),
        new Tag("trial", "Trial", "&5", 126, Optional.empty(), false),
        new Tag("mod", "Mod", "&5", 130, Optional.empty(), false),
        new Tag("mod+", "Mod+", "&5", 140, Optional.of("&5&lMOD&5+ "), false),
        new Tag("admin", "Admin", "&4", 150, Optional.empty(), false)
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
                nodes.add(PermissionNode.permanent(AFK_BYPASS, true, SEEDED_AT));
            }
            if (id.equals("ultra+")) {
                nodes.add(PermissionNode.permanent(PARTY_OPEN, true, SEEDED_AT));
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
            new Track("doacao", List.of("membro", "vip", "mvp", "pro", "ultra", "ultra+")),
            new Track("staff", List.of("builder", "helper", "trial", "mod", "mod+", "admin"))
        );
        return PermissionData.of(groups, TAGS, tracks, MEDALS);
    }
}
