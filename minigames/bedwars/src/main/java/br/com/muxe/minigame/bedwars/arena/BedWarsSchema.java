package br.com.muxe.minigame.bedwars.arena;

import br.com.muxe.minigame.engine.mode.GameMode;
import br.com.muxe.minigame.engine.setup.SetupSchema;
import br.com.muxe.minigame.engine.setup.SetupStep;
import br.com.muxe.minigame.engine.setup.StepKind;
import br.com.muxe.minigame.engine.setup.TeamRule;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;

public final class BedWarsSchema implements SetupSchema {

    public static final SetupStep WAITING_SPAWN = SetupStep.single("espera", "Spawn da espera", StepKind.LOCATION, false, "&e&lESPERA");
    public static final SetupStep WAITING_AREA = new SetupStep("area-espera", "Área da espera", StepKind.REGION, false, 2, 2,
        List.of("&e&lÁREA DA ESPERA", "&7Canto {index}"));
    public static final SetupStep SPECTATOR = SetupStep.single("espectador", "Spawn dos espectadores", StepKind.LOCATION, false, "&7&lESPECTADORES");
    public static final SetupStep VOID = SetupStep.single("void", "Altura do void", StepKind.HEIGHT, false);
    public static final SetupStep BUILD_LIMIT = SetupStep.optional("limite", "Altura máxima de construção", StepKind.HEIGHT, false);
    public static final SetupStep DIAMOND = SetupStep.many("diamante", "Gerador de diamante", StepKind.CENTER, false, 0, 16, "&b&lDIAMANTE #{index}");
    public static final SetupStep EMERALD = SetupStep.many("esmeralda", "Gerador de esmeralda", StepKind.CENTER, false, 0, 16, "&2&lESMERALDA #{index}");

    public static final SetupStep SPAWN = SetupStep.single("spawn", "Spawn", StepKind.LOCATION, true, "&a&lSPAWN", "{team}");
    public static final SetupStep BED = SetupStep.single("cama", "Cama", StepKind.BLOCK, true, "&c&lCAMA", "{team}");
    public static final SetupStep SHOP = SetupStep.single("loja", "Loja", StepKind.LOCATION, true, "&e&lLOJA", "{team}");
    public static final SetupStep UPGRADES = SetupStep.single("melhorias", "Melhorias", StepKind.LOCATION, true, "&b&lMELHORIAS", "{team}");
    public static final SetupStep IRON = SetupStep.many("ferro", "Gerador de ferro", StepKind.CENTER, true, 1, 4, "&f&lFERRO #{index}", "{team}");
    public static final SetupStep GOLD = SetupStep.many("ouro", "Gerador de ouro", StepKind.CENTER, true, 1, 4, "&6&lOURO #{index}", "{team}");

    public static final SetupStep EXTRA_SHOP = SetupStep.many("loja-extra", "Loja extra", StepKind.LOCATION, false, 0, 4, "&e&lLOJA EXTRA #{index}");
    public static final SetupStep EXTRA_UPGRADES = SetupStep.many("melhorias-extra", "Melhorias extras", StepKind.LOCATION, false, 0, 4, "&b&lMELHORIAS EXTRAS #{index}");
    public static final SetupStep EXTRA_IRON = SetupStep.many("ferro-extra", "Ferro extra", StepKind.CENTER, false, 0, 8, "&f&lFERRO EXTRA #{index}");
    public static final SetupStep EXTRA_GOLD = SetupStep.many("ouro-extra", "Ouro extra", StepKind.CENTER, false, 0, 8, "&6&lOURO EXTRA #{index}");

    private static final List<SetupStep> STEPS = List.of(
        WAITING_SPAWN, WAITING_AREA, SPECTATOR, VOID, BUILD_LIMIT, DIAMOND, EMERALD,
        SPAWN, BED, SHOP, UPGRADES, IRON, GOLD
    );
    private static final List<SetupStep> EXTRA_STEPS = List.of(EXTRA_SHOP, EXTRA_UPGRADES, EXTRA_IRON, EXTRA_GOLD);

    private final Map<String, GameMode> modes;
    private final Map<String, List<TeamColor>> fixedTeams;
    private final Set<String> extraIslands;

    public BedWarsSchema(List<GameMode> modes, Map<String, List<TeamColor>> fixedTeams, Set<String> extraIslands) {
        this.modes = modes.stream().collect(Collectors.toMap(GameMode::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        this.fixedTeams = Map.copyOf(fixedTeams);
        this.extraIslands = Set.copyOf(extraIslands);
        this.fixedTeams.forEach((mode, teams) -> {
            GameMode gameMode = this.modes.get(mode);
            if (gameMode != null && gameMode.teams() != teams.size()) {
                throw new IllegalArgumentException("Mode " + mode + " has " + gameMode.teams() + " teams but lists " + teams.size() + " fixed teams");
            }
        });
    }

    public static BedWarsSchema from(List<GameMode> modes, ConfigurationSection section) {
        Map<String, List<TeamColor>> fixed = new LinkedHashMap<>();
        List<String> extras = new ArrayList<>();
        for (GameMode mode : modes) {
            ConfigurationSection modeSection = section.getConfigurationSection(mode.id());
            List<TeamColor> teams = new ArrayList<>();
            for (String id : modeSection.getStringList("fixed-teams")) {
                teams.add(TeamColor.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown team " + id + " in mode " + mode.id())));
            }
            if (!teams.isEmpty()) {
                fixed.put(mode.id(), teams);
            }
            if (modeSection.getBoolean("extra-islands", false)) {
                extras.add(mode.id());
            }
        }
        return new BedWarsSchema(modes, fixed, Set.copyOf(extras));
    }

    public GameMode mode(String id) {
        GameMode mode = modes.get(id);
        if (mode == null) {
            throw new IllegalArgumentException("Unknown mode " + id);
        }
        return mode;
    }

    @Override
    public List<String> modes() {
        return List.copyOf(modes.keySet());
    }

    @Override
    public TeamRule teams(String mode) {
        List<TeamColor> fixed = fixedTeams.get(mode);
        return fixed == null ? TeamRule.chosen(mode(mode).teams()) : new TeamRule(fixed, fixed.size());
    }

    @Override
    public List<SetupStep> steps(String mode) {
        if (!extraIslands.contains(mode)) {
            return STEPS;
        }
        List<SetupStep> steps = new ArrayList<>(STEPS);
        steps.addAll(EXTRA_STEPS);
        return List.copyOf(steps);
    }
}
