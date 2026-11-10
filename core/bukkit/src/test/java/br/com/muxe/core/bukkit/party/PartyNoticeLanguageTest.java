package br.com.muxe.core.bukkit.party;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PartyNoticeLanguageTest {

    private final TestNetwork network = new TestNetwork();
    private final FakeSender portuguese = FakeSender.named("tadeu");
    private final FakeSender english = FakeSender.named("steve");
    private final UUID englishId = english.player().getUniqueId();
    private final MessageCatalog messages = MessageCatalog.from(LocalizedConfig.bundled(PartyTexts.class, "config.yml"), "messages")
        .speaking(player -> player.equals(englishId) ? Language.ENGLISH : Language.PORTUGUESE);
    private final Map<UUID, Player> online = Map.of(portuguese.player().getUniqueId(), portuguese.player(), englishId, english.player());
    private final PartyClient client = new PartyClient(network.node("lobby-1", "lobby", ServerKind.LOBBY).bus(), Runnable::run, "lobby-1",
        Duration.ofSeconds(1), new PartyTexts(messages, "{player}: {message}"), id -> Optional.ofNullable(online.get(id)), new NetworkPlayers(List::of),
        Logger.getLogger("test"));

    @AfterEach
    void close() {
        network.close();
    }

    @Test
    void eachRecipientReadsTheNoticeInTheirOwnLanguage() {
        client.deliver(PartyNotice.of(List.copyOf(online.keySet()), "party-kicked", "leader", "&5tadeu", "player", "&7ana"));

        assertThat(portuguese.messages()).containsExactly("§d[PARTY] §5tadeu §cremoveu §7ana §cda party.");
        assertThat(english.messages()).containsExactly("§d[PARTY] §5tadeu §cremoved §7ana §cfrom the party.");
    }

    @Test
    void theInviteIsTranslatedAndKeepsItsClickCommand() {
        PartyNotice invite = PartyNotice.of(List.of(englishId), "party-invite", "player", "&5tadeu").clicking("/party aceitar tadeu");

        assertThat(client.texts().lines(Language.ENGLISH, invite)).containsExactly("§5tadeu §einvited you to a party", "§b§lCLICK HERE §eto accept the invite.");
        assertThat(invite.clickCommand()).contains("/party aceitar tadeu");
    }
}
