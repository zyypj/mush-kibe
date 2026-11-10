package br.com.muxe.core.proxy.language;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.cqrs.bus.SimpleQueryBus;
import br.com.muxe.core.common.language.application.query.FindLanguageHandler;
import br.com.muxe.core.common.language.application.query.FindLanguageQuery;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.messaging.channel.PacketTarget;
import br.com.muxe.core.common.network.language.LanguageChangedPacket;
import br.com.muxe.core.common.server.ServerKind;
import br.com.muxe.core.common.testing.DirectTransactions;
import br.com.muxe.core.common.testing.InMemoryLanguageRepository;
import br.com.muxe.core.common.testing.TestNetwork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProxyLanguagesTest {

    private final InMemoryLanguageRepository repository = new InMemoryLanguageRepository();
    private final SimpleQueryBus queries = new SimpleQueryBus(Runnable::run, List.of());
    private final ProxyLanguages languages = new ProxyLanguages(queries, Logger.getLogger("test"));
    private final TestNetwork network = new TestNetwork();
    private final TestNetwork.Node proxy = network.node("proxy-1", "proxy", ServerKind.PROXY);
    private final TestNetwork.Node lobby = network.node("lobby-1", "lobby", ServerKind.LOBBY);
    private final UUID player = UUID.randomUUID();

    ProxyLanguagesTest() {
        queries.register(FindLanguageQuery.class, new FindLanguageHandler(repository, new DirectTransactions()));
    }

    @AfterEach
    void tearDown() {
        network.close();
    }

    @Test
    void aPlayerIsPortugueseUntilTheirLanguageIsLoaded() {
        repository.save(player, Language.ENGLISH, Instant.EPOCH);

        assertThat(languages.of(player)).isEqualTo(Language.PORTUGUESE);

        assertThat(languages.load(player).join()).isEqualTo(Language.ENGLISH);
        assertThat(languages.of(player)).isEqualTo(Language.ENGLISH);
    }

    @Test
    void aPlayerWhoNeverChoseALanguageIsPortuguese() {
        assertThat(languages.load(player).join()).isEqualTo(Language.PORTUGUESE);
        assertThat(languages.of(player)).isEqualTo(Language.PORTUGUESE);
    }

    @Test
    void aFailedLoadFallsBackToPortuguese() {
        ProxyLanguages broken = new ProxyLanguages(new SimpleQueryBus(Runnable::run, List.of()), Logger.getLogger("test"));

        assertThat(broken.load(player).join()).isEqualTo(Language.PORTUGUESE);
        assertThat(broken.of(player)).isEqualTo(Language.PORTUGUESE);
    }

    @Test
    void aLanguageChangeFromAServerUpdatesTheOnlinePlayerAndTellsTheListeners() {
        List<UUID> changed = new ArrayList<>();
        languages.onChange(changed::add);
        languages.register(proxy.bus());
        start();
        languages.load(player).join();

        lobby.bus().publish(PacketTarget.proxy(), new LanguageChangedPacket(player, Language.ENGLISH)).join();

        assertThat(languages.of(player)).isEqualTo(Language.ENGLISH);
        assertThat(changed).containsExactly(player);
    }

    @Test
    void aLanguageChangeOfAPlayerWhoIsNotHereIsIgnored() {
        List<UUID> changed = new ArrayList<>();
        languages.onChange(changed::add);
        languages.register(proxy.bus());
        start();

        lobby.bus().publish(PacketTarget.proxy(), new LanguageChangedPacket(player, Language.ENGLISH)).join();

        assertThat(languages.of(player)).isEqualTo(Language.PORTUGUESE);
        assertThat(changed).isEmpty();
    }

    @Test
    void aPlayerWhoLeftIsForgotten() {
        repository.save(player, Language.ENGLISH, Instant.EPOCH);
        languages.load(player).join();

        languages.forget(player);

        assertThat(languages.of(player)).isEqualTo(Language.PORTUGUESE);
        languages.change(player, Language.ENGLISH);
        assertThat(languages.of(player)).isEqualTo(Language.PORTUGUESE);
    }

    private void start() {
        proxy.start();
        lobby.start();
    }
}
