package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.proxy.text.ProxyMessages;
import java.nio.file.Path;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProxyConfigFileTest {

    @TempDir
    private Path folder;

    @Test
    void theClosedNetworkScreenFollowsTheKickScreenLayout() {
        MapConfigSource config = ProxyConfigFile.load(folder);
        ProxyMessages messages = new ProxyMessages(config);

        String expected = String.join("\n",
            "&cA rede está temporariamente fechada.",
            "&cMotivo: Servidores de autenticação desligados",
            "&cTente novamente em alguns minutos.",
            "",
            "&eAcompanhe as novidades em: &bmuxe.com.br",
            "&cO problema continua? Avise a equipe em &bmuxe.com.br"
        );

        assertThat(messages.get("no-auth-server")).isEqualTo(LegacyComponentSerializer.legacyAmpersand().deserialize(expected));
    }

    @Test
    void theDefaultFileIsWrittenOnFirstStart() {
        ProxyConfigFile.load(folder);

        assertThat(folder.resolve(ProxyConfigFile.FILE_NAME)).exists();
    }
}
