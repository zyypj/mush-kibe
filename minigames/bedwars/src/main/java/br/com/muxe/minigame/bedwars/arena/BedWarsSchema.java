package br.com.muxe.minigame.bedwars.arena;

import br.com.muxe.minigame.api.arena.ArenaSchema;
import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.LinkedBlock;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class BedWarsSchema {

    public static final SetupStep WAITING_SPAWN = ArenaSchema.WAITING_SPAWN;
    public static final SetupStep WAITING_AREA = ArenaSchema.WAITING_AREA;
    public static final SetupStep SPECTATOR = ArenaSchema.SPECTATOR;
    public static final SetupStep VOID = ArenaSchema.VOID;
    public static final SetupStep BUILD_LIMIT = SetupStep.optional("limite", "Altura máxima de construção", StepKind.HEIGHT, false);
    public static final SetupStep DIAMOND = SetupStep.many("diamante", "Gerador de diamante", StepKind.CENTER, false, 0, 16, "&b&lDIAMANTE #{index}");
    public static final SetupStep EMERALD = SetupStep.many("esmeralda", "Gerador de esmeralda", StepKind.CENTER, false, 0, 16, "&2&lESMERALDA #{index}");

    public static final SetupStep SPAWN = ArenaSchema.SPAWN;
    public static final SetupStep BED = SetupStep.single("cama", "Cama", StepKind.BLOCK, true, "&c&lCAMA", "{team}");
    public static final SetupStep SHOP = SetupStep.single("loja", "Loja", StepKind.LOCATION, true, "&e&lLOJA", "{team}");
    public static final SetupStep UPGRADES = SetupStep.single("melhorias", "Melhorias", StepKind.LOCATION, true, "&b&lMELHORIAS", "{team}");
    public static final SetupStep IRON = SetupStep.many("ferro", "Gerador de ferro", StepKind.CENTER, true, 1, 4, "&f&lFERRO #{index}", "{team}");
    public static final SetupStep GOLD = SetupStep.many("ouro", "Gerador de ouro", StepKind.CENTER, true, 1, 4, "&6&lOURO #{index}", "{team}");

    public static final SetupStep EXTRA_SHOP = SetupStep.many("loja-extra", "Loja extra", StepKind.LOCATION, false, 0, 4, "&e&lLOJA EXTRA #{index}");
    public static final SetupStep EXTRA_UPGRADES = SetupStep.many("melhorias-extra", "Melhorias extras", StepKind.LOCATION, false, 0, 4, "&b&lMELHORIAS EXTRAS #{index}");
    public static final SetupStep EXTRA_IRON = SetupStep.many("ferro-extra", "Ferro extra", StepKind.CENTER, false, 0, 8, "&f&lFERRO EXTRA #{index}");
    public static final SetupStep EXTRA_GOLD = SetupStep.many("ouro-extra", "Ouro extra", StepKind.CENTER, false, 0, 8, "&6&lOURO EXTRA #{index}");

    private static final String BED_MATERIAL = "BED_BLOCK";
    private static final int BED_SEARCH_RADIUS = 20;

    private BedWarsSchema() {
    }

    public static ArenaSchema from(List<GameMode> modes, ConfigurationSection section) {
        Set<String> extraIslands = new HashSet<>();
        Set<String> islands = new HashSet<>();
        for (GameMode mode : modes) {
            ConfigurationSection modeSection = section.getConfigurationSection(mode.id());
            if (modeSection != null && modeSection.getBoolean("extra-islands", false)) {
                extraIslands.add(mode.id());
            }
            if (modeSection == null || modeSection.getBoolean("islands", true)) {
                islands.add(mode.id());
            }
        }
        return ArenaSchema.builder(modes)
            .waiting()
            .spectator()
            .voidHeight()
            .step(BUILD_LIMIT, DIAMOND, EMERALD)
            .teamSpawn()
            .step(BED)
            .stepsFor(islands, SHOP, UPGRADES, IRON, GOLD)
            .stepsFor(extraIslands, EXTRA_SHOP, EXTRA_UPGRADES, EXTRA_IRON, EXTRA_GOLD)
            .linked(new LinkedBlock(BED, Set.of(BED_MATERIAL), BED_SEARCH_RADIUS))
            .fixedTeams(section)
            .build();
    }
}
