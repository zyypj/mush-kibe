package br.com.muxe.core.proxy.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.config.source.MapConfigSource;
import br.com.muxe.core.proxy.text.ProxyMessages;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
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
            "&cNão foi possível te conectar ao servidor.",
            "&cMotivo: Servidores de Autenticação Desligados",
            "&cTente novamente em alguns minutos.",
            "",
            "&eAcompanhe as notícias em: &bmuxe.com.br",
            "&cO problema continua? Avise a equipe em &bdiscord.muxe.com.br"
        );

        assertThat(messages.get("no-auth-server")).isEqualTo(LegacyComponentSerializer.legacyAmpersand().deserialize(expected));
    }

    @Test
    void theRestartScreenFollowsTheKickScreenLayout() {
        ProxyMessages messages = new ProxyMessages(ProxyConfigFile.load(folder));

        String expected = String.join("\n",
            "&cVocê foi desconectado do servidor.",
            "&cMotivo: Servidor reiniciando",
            "&cTente entrar novamente em alguns minutos.",
            "",
            "&eMais informações no nosso Discord: &bdiscord.muxe.com.br"
        );

        assertThat(messages.get("server-restarting")).isEqualTo(LegacyComponentSerializer.legacyAmpersand().deserialize(expected));
    }

    @Test
    void aPasswordLoginIsRememberedForADay() {
        MapConfigSource config = ProxyConfigFile.load(folder);

        assertThat(AuthenticationSettings.from(config).sessionDuration()).isEqualTo(Duration.ofHours(24));
        assertThat(AuthenticationSettings.from(new MapConfigSource(Map.of())).sessionDuration()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void theDefaultFileIsWrittenOnFirstStart() {
        ProxyConfigFile.load(folder);

        assertThat(folder.resolve(ProxyConfigFile.FILE_NAME)).exists();
    }
}
