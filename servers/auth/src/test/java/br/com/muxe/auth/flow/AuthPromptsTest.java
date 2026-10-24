package br.com.muxe.auth.flow;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class AuthPromptsTest {

    private static final String BLANK = MessageCatalog.BLANK_LINE;

    private final AuthPrompts prompts = new AuthPrompts(defaultMessages(), 20);

    @Test
    void theChallengeIsOneBlockWithABlankLineBeforeTheHint() {
        assertThat(prompts.challengeLines()).containsExactly(
            "§6§lDESAFIO:",
            "§eDigite o §bcódigo §eem frente §ano chat§e!",
            BLANK,
            "§7§o(É necessário ativar os hologramas para visualizar corretamente)"
        );
    }

    @Test
    void afterTheChallengeTheWarningComesFirstAndTheSuccessLast() {
        assertThat(prompts.credentialLines(true, true)).containsExactly(
            "§cNUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!",
            BLANK,
            "§eFaça o login utilizando §b/logar <senha>",
            "§aDesafio completado com sucesso!"
        );
    }

    @Test
    void withoutAChallengeThereIsNoSuccessLine() {
        assertThat(prompts.credentialLines(false, false)).containsExactly(
            "§cNUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!",
            BLANK,
            "§eFaça o registro utilizando §b/registrar <senha> <senha>"
        );
    }

    @Test
    void authenticatingSaysWhereThePlayerGoes() {
        assertThat(prompts.authenticatedLines(true)).containsExactly("§aVocê se autenticou com sucesso!", "§aEnviando ao lobby...");
    }

    private static MessageCatalog defaultMessages() {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(AuthPromptsTest.class.getResourceAsStream("/config.yml")), StandardCharsets.UTF_8)) {
            return MessageCatalog.from(YamlConfiguration.loadConfiguration(reader).getConfigurationSection("messages"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
