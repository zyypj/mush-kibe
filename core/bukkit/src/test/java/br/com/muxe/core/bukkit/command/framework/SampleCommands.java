package br.com.muxe.core.bukkit.command.framework;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

final class SampleCommands {

    static final String WARP = "test.warp";
    static final String ANNOUNCE = "test.announce";

    private SampleCommands() {
    }

    enum Mode {
        CRIATIVO,
        SOBREVIVENCIA_EXTREMA
    }

    @Command(name = "warp", aliases = "w", description = "Warps.")
    @Permission(WARP)
    public static final class Warp {

        final List<String> calls = new ArrayList<>();

        @Default
        public void list(CommandSender sender) {
            calls.add("list");
        }

        @Subcommand(value = "criar", aliases = "create")
        public void create(Player player, @Arg("nome") String name, @Arg(value = "custo", defaultValue = "10") int cost) {
            calls.add("create " + name + " " + cost);
        }

        @Subcommand("ir")
        public void go(Player player, @Suggest(value = {"norte", "sul"}, provider = "places") String place) {
            calls.add("go " + place);
        }

        @Suggestions("places")
        public List<String> places() {
            return List.of("spawn", "loja");
        }
    }

    @Command(name = "painel")
    public static final class Panel {

        final List<String> calls = new ArrayList<>();

        @Subcommand("anunciar")
        @Permission(ANNOUNCE)
        public void announce(CommandSender sender, @Greedy String text) {
            calls.add("announce " + text);
        }

        @Subcommand("modo definir")
        public void mode(CommandSender sender, Mode mode, boolean on) {
            calls.add("mode " + mode + " " + on);
        }

        @Subcommand("modo limite")
        public void limit(CommandSender sender, long max, double ratio) {
            calls.add("limit " + max + " " + ratio);
        }

        @Subcommand("nota")
        public void note(CommandSender sender, @Arg("texto") @Greedy Optional<String> text) {
            calls.add("note " + text.orElse("-"));
        }

        @Subcommand("parar")
        public void stop(ConsoleCommandSender console) {
            calls.add("stop");
        }

        @Subcommand("erro")
        public void fail(CommandSender sender) {
            throw new IllegalStateException("boom");
        }

        @Subcommand("jogador")
        public void player(CommandSender sender, @Suggest(provider = "friends") String name) {
            calls.add("player " + name);
        }
    }

    @Command(name = "casa", usage = "custom-usage")
    public static final class Home {

        final List<String> calls = new ArrayList<>();

        @Subcommand("ir")
        public void go(Player player, String name) {
            calls.add("go " + name);
        }
    }

    @Command(name = "ajuda")
    public static final class Help {

        final List<String> calls = new ArrayList<>();

        @Subcommand("ver")
        public void show(CommandSender sender, int page) {
            calls.add("show " + page);
        }

        @Usage
        public void usage(CommandSender sender) {
            calls.add("usage");
        }
    }

    public static final class Pair {

        final List<String> calls = new ArrayList<>();

        @Command(name = "entrar", aliases = "login", usage = "custom-usage")
        public void login(Player player, String password) {
            calls.add("login " + password);
        }

        @Command(name = "sair")
        public void logout(Player player) {
            calls.add("logout");
        }
    }

    public static final class Unannotated {

        public void nothing() {
        }
    }

    @Command(name = "ruim")
    public static final class RequiredAfterOptional {

        @Default
        public void run(CommandSender sender, Optional<String> first, String second) {
        }
    }

    @Command(name = "ruim")
    public static final class GreedyFirst {

        @Default
        public void run(CommandSender sender, @Greedy String text, String other) {
        }
    }

    @Command(name = "ruim")
    public static final class UnknownType {

        @Default
        public void run(CommandSender sender, StringBuilder text) {
        }
    }

    @Command(name = "ruim")
    public static final class UnknownProvider {

        @Default
        public void run(CommandSender sender, @Suggest(provider = "nada") String text) {
        }
    }

    @Command(name = "ruim")
    public static final class Duplicated {

        @Subcommand("a")
        public void first(CommandSender sender) {
        }

        @Subcommand("A")
        public void second(CommandSender sender) {
        }
    }
}
