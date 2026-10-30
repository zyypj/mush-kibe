package br.com.muxe.minigame.engine.setup.session;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.minigame.engine.setup.ArenaDraft;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.bukkit.entity.Player;

@Command(name = SetupCommand.NAME, description = "Sets up the arenas of this game.", usage = "setup-usage")
@Permission(SetupService.PERMISSION)
public final class SetupCommand {

    public static final String NAME = "arena";

    private static final String STEPS = "steps";
    private static final String TEAMS = "teams";
    private static final String COLORS = "colors";
    private static final String MODES = "modes";
    private static final String ARENAS = "arenas";

    private final SetupService setup;

    public SetupCommand(SetupService setup) {
        this.setup = setup;
    }

    @Default
    public void help(Player player) {
        setup.help(player);
    }

    @Subcommand(value = "criar", aliases = "editar")
    public void open(Player player, @Arg("nome") @Suggest(provider = ARENAS) String name, @Arg("modo") @Suggest(provider = MODES) Optional<String> mode) {
        setup.open(player, name, mode);
    }

    @Subcommand("ajuda")
    public void helpCommand(Player player) {
        setup.help(player);
    }

    @Subcommand("definir")
    public void define(Player player, @Arg("passo") @Suggest(provider = STEPS) String step, @Arg("time") @Suggest(provider = TEAMS) Optional<String> team) {
        setup.define(player, step, team);
    }

    @Subcommand("remover")
    public void remove(Player player, @Arg("passo") @Suggest(provider = STEPS) String step, @Arg("time") @Suggest(provider = TEAMS) Optional<String> team) {
        setup.remove(player, step, team);
    }

    @Subcommand("time")
    public void team(Player player, @Arg("ação") @Suggest({"adicionar", "remover"}) String action, @Arg("cor") @Suggest(provider = COLORS) String color) {
        setup.team(player, !action.equalsIgnoreCase("remover"), color);
    }

    @Subcommand("nome")
    public void rename(Player player, @Arg("nome") @Greedy String name) {
        setup.rename(player, name);
    }

    @Subcommand("salvar")
    public void save(Player player) {
        setup.save(player);
    }

    @Subcommand("sair")
    public void leave(Player player) {
        setup.leave(player);
    }

    @Subcommand("listar")
    public void list(Player player) {
        setup.list(player);
    }

    @Suggestions(STEPS)
    public List<String> steps(Player player) {
        return setup.stepIds(player);
    }

    @Suggestions(TEAMS)
    public List<String> teams(Player player) {
        return setup.teamIds(player);
    }

    @Suggestions(COLORS)
    public List<String> colors() {
        return Arrays.stream(TeamColor.values()).map(TeamColor::id).toList();
    }

    @Suggestions(MODES)
    public List<String> modes() {
        return setup.schema().modes();
    }

    @Suggestions(ARENAS)
    public List<String> arenas() {
        try {
            return setup.arenas().names().stream().filter(ArenaDraft::isValidName).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }
}
