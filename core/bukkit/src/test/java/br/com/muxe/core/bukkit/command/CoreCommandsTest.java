package br.com.muxe.core.bukkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.command.framework.TestCommands;
import br.com.muxe.core.bukkit.command.framework.argument.ArgumentTypes;
import br.com.muxe.core.bukkit.command.framework.completion.SuggestionRegistry;
import br.com.muxe.core.bukkit.shop.ShopFixtures;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.HologramPlacement;
import br.com.muxe.core.common.placement.domain.NpcAction;
import br.com.muxe.core.common.placement.domain.NpcPlacement;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.placement.domain.Position;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoreCommandsTest {

    private static final MessageCatalog MESSAGES = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));
    private static final Position HERE = new Position("lobby", 0, 64, 0, 0f, 0f);
    private static final Placements PLACEMENTS = new Placements(
        List.of(new NpcPlacement("lobby", "vendedor", HERE, "Vendedor", NpcAction.SHOP, "", Optional.empty())),
        List.of(new HologramPlacement("lobby", "boas-vindas", HERE, List.of("&bOlá"))),
        List.of(),
        List.of(),
        List.of()
    );

    private TestCommands commands;

    @BeforeEach
    void register() {
        commands = new TestCommands(ArgumentTypes.basic(), MESSAGES);
        commands.commands().suggestions(SuggestionRegistry.PLAYERS, sender -> List.of("Ana"));
        PlacementArgument.registerAll(commands.commands(), () -> PLACEMENTS);
        commands.register(
            new NpcCommand(null, null, null, MESSAGES, null),
            new HologramCommand(null, MESSAGES, null),
            new SocialHeadCommand(null, MESSAGES, null),
            new ParkourCommand(null, MESSAGES, null),
            new ShopCommand(Optional.empty(), null, MESSAGES, null),
            new BuildCommand(null, MESSAGES),
            new LobbyCommand(null, "lobby"),
            new MuxeAdminCommand(null)
        );
    }

    @Test
    void placementCommandsKeepTheirSenderPermissionAndUsageReplies() {
        FakeSender console = FakeSender.named("CONSOLE", NpcCommand.PERMISSION);
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", NpcCommand.PERMISSION);

        commands.run(console.console(), "npc");
        commands.run(player.player(), "npc");
        commands.run(staff.player(), "npc");
        commands.run(staff.player(), "npc criar guia loja Notch");

        assertThat(console.messages()).containsExactly(MESSAGES.get("players-only"));
        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"));
        assertThat(staff.messages()).containsExactlyElementsOf(twice(MESSAGES.lines("npc-usage")));
    }

    @Test
    void unknownPlacementsAreReportedWithTheirLowercaseId() {
        FakeSender staff = FakeSender.named("Staff", NpcCommand.PERMISSION, HologramCommand.PERMISSION, SocialHeadCommand.PERMISSION);

        commands.run(staff.player(), "npc mover Guia");
        commands.run(staff.player(), "holo ver nada");
        commands.run(staff.player(), "redes remover x");

        assertThat(staff.messages()).containsExactly(
            MESSAGES.get("npc-unknown", "id", "guia"),
            MESSAGES.get("hologram-unknown", "id", "nada"),
            MESSAGES.get("social-unknown", "id", "x")
        );
    }

    @Test
    void lineNumbersAreCheckedAgainstTheHologram() {
        FakeSender staff = FakeSender.named("Staff", HologramCommand.PERMISSION);

        commands.run(staff.player(), "holograma removerlinha boas-vindas 3");
        commands.run(staff.player(), "holograma linha boas-vindas um texto");
        commands.run(staff.player(), "holograma linha boas-vindas 1");

        assertThat(staff.messages()).containsExactlyElementsOf(concat(
            List.of(MESSAGES.get("hologram-invalid-line", "count", "1"), MESSAGES.get("command-invalid-number", "input", "um")),
            MESSAGES.lines("hologram-usage")
        ));
    }

    @Test
    void theParkourRejectsPointsItDoesNotKnow() {
        FakeSender staff = FakeSender.named("Staff", ParkourCommand.PERMISSION);

        commands.run(staff.player(), "parkour remover");
        commands.run(staff.player(), "parkour checkpoint zero");
        commands.run(staff.player(), "parkour remover meio");

        assertThat(staff.messages()).containsExactly(
            MESSAGES.get("parkour-invalid-point"),
            MESSAGES.get("parkour-invalid-point"),
            MESSAGES.get("parkour-invalid-point")
        );
    }

    @Test
    void theShopOpensForEveryoneButOnlyStaffSeeTheUsage() {
        FakeSender player = FakeSender.named("Steve");
        FakeSender staff = FakeSender.named("Staff", ShopCommand.PERMISSION);

        commands.run(player.player(), "loja");
        commands.run(player.player(), "shop caixa");
        commands.run(player.player(), "loja qualquer");
        commands.run(staff.player(), "loja qualquer");

        assertThat(player.messages()).containsExactly(MESSAGES.get("shop-unavailable"), MESSAGES.get("no-permission"), MESSAGES.get("no-permission"));
        assertThat(staff.messages()).containsExactlyElementsOf(MESSAGES.lines("shop-usage"));
    }

    @Test
    void networkCommandsKeepTheirReplies() {
        FakeSender console = FakeSender.named("CONSOLE", MuxeAdminCommand.PERMISSION);
        FakeSender player = FakeSender.named("Steve");

        commands.run(console.console(), "muxe");
        commands.run(console.console(), "muxe find");
        commands.run(console.console(), "hub");
        commands.run(console.console(), "build");
        commands.run(player.player(), "muxe servers");

        assertThat(console.messages()).containsExactly(
            MESSAGES.get("admin.usage"),
            MESSAGES.get("admin.usage"),
            MESSAGES.get("players-only"),
            MESSAGES.get("players-only")
        );
        assertThat(player.messages()).containsExactly(MESSAGES.get("no-permission"));
    }

    @Test
    void tabCompletesSubcommandsPlacementsAndChoices() {
        FakeSender staff = FakeSender.named("Staff", NpcCommand.PERMISSION, HologramCommand.PERMISSION, ParkourCommand.PERMISSION, "muxe.build");
        FakeSender player = FakeSender.named("Steve");

        assertThat(commands.complete(staff.player(), "npc ")).containsExactly("acao", "criar", "listar", "mover", "nome", "remover", "skin");
        assertThat(commands.complete(staff.player(), "npc mover ")).containsExactly("vendedor");
        assertThat(commands.complete(staff.player(), "npc criar guia ")).containsExactly("loja", "nenhuma");
        assertThat(commands.complete(staff.player(), "holograma ver b")).containsExactly("boas-vindas");
        assertThat(commands.complete(staff.player(), "parkour remover ")).containsExactly("checkpoint", "fim", "inicio");
        assertThat(commands.complete(staff.player(), "parkour i")).containsExactly("inicio", "início");
        assertThat(commands.complete(staff.player(), "build ")).containsExactly("off", "on");
        assertThat(commands.complete(player.player(), "npc ")).isEmpty();
        assertThat(commands.complete(player.player(), "loja ")).isEmpty();
        assertThat(commands.complete(FakeSender.named("CONSOLE", MuxeAdminCommand.PERMISSION).console(), "muxe find ")).containsExactly("Ana");
    }

    private static List<String> twice(List<String> lines) {
        return concat(lines, lines);
    }

    private static List<String> concat(List<String> first, List<String> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
