package br.com.muxe.core.common.customroom.schema;

import br.com.muxe.core.common.customroom.schema.OptionEntry.Choice;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ChoiceValue;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Decimal;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Format;
import br.com.muxe.core.common.customroom.schema.OptionEntry.FreeAll;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Level;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Link;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ShopItem;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Stepper;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Team;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Timer;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Toggle;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Upgrade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class SchemaFixtures {

    static final Stepper TEAM_SIZE = new Stepper(10, Icon.of("SKULL_ITEM", 3), "Tamanho do time", List.of("&7A quantidade de jogadores", "&7por time."),
        "team-size", "mode-team-size", "1", "mode-players", 1, Format.INTEGER, false);
    static final Stepper TEAM_COUNT = new Stepper(10, Icon.of("BANNER"), "Quantidade de times", List.of("&7Aumente a quantidade", "&7de times disponíveis", "&7no mapa."),
        "teams.count", "mode-teams", "2", "mode-teams", 1, Format.INTEGER, true);
    static final Stepper DIAMOND_SPEED = new Stepper(12, Icon.of("DIAMOND"), "Velocidade de Diamante", List.of("&7Altera a velocidade", "&7dos geradores de Diamante."),
        "generators.diamond.speed", "1", "0.25", "5", 0.25, Format.MULTIPLIER, false);
    static final Toggle DIAMOND = new Toggle(11, Icon.of("DIAMOND_BLOCK"), "Gerador de Diamante", List.of("&7Ativa ou desativa os", "&7geradores de Diamante."),
        "generators.diamond.enabled", true);
    static final Toggle RESTORE = new Toggle(20, Icon.of("ANVIL"), "Restaurar impulso", List.of("&7Se o impulso anterior", "&7é restaurado após ser", "&7enviado ao jogador."),
        "knockback.restore-velocity", false);
    static final Decimal VERTICAL = new Decimal(10, Icon.of("FEATHER"), "Vertical", List.of("&7Valor base do impulso", "&7vertical."), "knockback.vertical", "0.6", 0, 5);
    static final Choice LIMITER = new Choice(25, "Limitador de cliques", List.of("&7Controla a força do", "&7limitador de cliques", "&7de knockback."), "knockback.click-limiter",
        List.of(new ChoiceValue("off", "Desativado", Icon.of("STONE_BUTTON")), new ChoiceValue("low", "Baixo", Icon.of("STONE_BUTTON")),
            new ChoiceValue("normal", "Normal", Icon.of("STONE_BUTTON")), new ChoiceValue("high", "Alto", Icon.of("STONE_BUTTON"))), "normal");
    static final Timer DIAMOND_TWO = new Timer(10, Icon.of("DIAMOND"), "Diamante II", "events.diamond-2", 360, true);
    static final Timer GAME_END = new Timer(16, Icon.of("WATCH"), "Fim do Jogo", "events.game-end", 300, false);
    static final Team FIRST_TEAM = new Team(11, 1);
    static final ShopItem WOOL = new ShopItem(10, "la", Icon.of("WOOL").withAmount(16), "Lã", List.of("&7Ótimo para fazer pontes."), 4, "iron", true);
    static final ShopItem STONE_SWORD = new ShopItem(19, "espada-pedra", Icon.of("STONE_SWORD"), "Espada de Pedra", List.of(), 10, "iron", true);
    static final ShopItem FROZEN_SWAP = new ShopItem(20, "troca-congelante", Icon.of("BARRIER"), "Troca Congelante", List.of(), 2, "emerald", false);
    static final Upgrade ARMOR = new Upgrade(11, "reforco", Icon.of("IRON_CHESTPLATE"), "Reforço na Armadura",
        List.of("&7O seu time ganha Proteção em todas", "&7as peças armaduras."),
        List.of(new Level("Proteção I", 2, "diamond"), new Level("Proteção II", 4, "diamond"), new Level("Proteção III", 8, "diamond"),
            new Level("Proteção IV", 16, "diamond")), true);
    static final FreeAll SHOP_FREE = new FreeAll(44, "shop", "&7Altera o preço de todos os itens da loja.");
    static final Link TEAMS_LINK = new Link(11, Icon.of("BANNER"), "Times", List.of("&7Altera a quantidade", " ", "&eClique para ver mais."), "teams");

    static final ModeContext SOLO = new ModeContext("solo", 1, 8, 8);

    private SchemaFixtures() {
    }

    static RoomSchema schema() {
        Map<String, OptionPage> pages = new LinkedHashMap<>();
        pages.put("options", new OptionPage("options", "Opções", 4, 30, "", List.of(TEAM_SIZE, TEAMS_LINK), Optional.empty(), Optional.empty()));
        pages.put("teams", new OptionPage("teams", "Times", 4, 30, "options", List.of(TEAM_COUNT, FIRST_TEAM), Optional.empty(), Optional.empty()));
        pages.put("generators", new OptionPage("generators", "Geradores", 3, 21, "options", List.of(DIAMOND, DIAMOND_SPEED), Optional.empty(), Optional.empty()));
        pages.put("knockback", new OptionPage("knockback", "Alterar knockback", 4, 30, "options", List.of(VERTICAL, RESTORE, LIMITER), Optional.empty(), Optional.empty()));
        pages.put("events", new OptionPage("events", "Eventos", 4, 31, "options", List.of(DIAMOND_TWO, GAME_END), Optional.empty(), Optional.empty()));
        pages.put("shop", new OptionPage("shop", "Itens da loja (1/1)", 5, 39, "options", List.of(WOOL, STONE_SWORD, SHOP_FREE), Optional.empty(), Optional.empty()));
        pages.put("upgrades", new OptionPage("upgrades", "Melhorias (1/1)", 5, 39, "options", List.of(ARMOR), Optional.empty(), Optional.empty()));
        Map<String, Currency> currencies = new LinkedHashMap<>();
        currencies.put("iron", new Currency("iron", "Ferro", "Ferros", "&f", Icon.of("IRON_INGOT")));
        currencies.put("gold", new Currency("gold", "Ouro", "Ouros", "&6", Icon.of("GOLD_INGOT")));
        currencies.put("diamond", new Currency("diamond", "Diamante", "Diamantes", "&b", Icon.of("DIAMOND")));
        currencies.put("emerald", new Currency("emerald", "Esmeralda", "Esmeraldas", "&2", Icon.of("EMERALD")));
        return new RoomSchema("bedwars", "options", pages, currencies);
    }

    static RoomEditor editor() {
        return new RoomEditor(schema(), SOLO);
    }

    static RoomLore lore() {
        return new RoomLore(editor(), RoomTexts.defaults());
    }
}
