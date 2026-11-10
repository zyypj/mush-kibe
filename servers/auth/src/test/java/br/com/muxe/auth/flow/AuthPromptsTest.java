package br.com.muxe.auth.flow;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.auth.MuxeAuthPlugin;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.bukkit.text.Title;
import br.com.muxe.core.common.language.domain.Language;
import org.junit.jupiter.api.Test;

class AuthPromptsTest {

    private static final String BLANK = MessageCatalog.BLANK_LINE;

    private final AuthPrompts prompts = new AuthPrompts(defaultMessages(), 20);

    @Test
    void theChallengeIsOneBlockWithABlankLineBeforeTheHint() {
        assertThat(prompts.challengeLines(Language.PORTUGUESE)).containsExactly(
            "§6§lDESAFIO:",
            "§eDigite o §bcódigo §eem frente §ano chat§e!",
            BLANK,
            "§7§o(É necessário ativar os hologramas para visualizar corretamente)"
        );
    }

    @Test
    void afterTheChallengeTheWarningComesFirstAndTheSuccessLast() {
        assertThat(prompts.credentialLines(Language.PORTUGUESE, true, true)).containsExactly(
            "§c§lNUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!",
            BLANK,
            "§eFaça o login utilizando §b/logar <senha>",
            "§aDesafio completado com sucesso!"
        );
    }

    @Test
    void withoutAChallengeThereIsNoSuccessLine() {
        assertThat(prompts.credentialLines(Language.PORTUGUESE, false, false)).containsExactly(
            "§c§lNUNCA COMPARTILHE A SUA SENHA COM NINGUÉM!",
            BLANK,
            "§eFaça o registro utilizando §b/registrar <senha> <senha>"
        );
    }

    @Test
    void authenticatingSaysWhereThePlayerGoes() {
        assertThat(prompts.authenticatedLines(Language.PORTUGUESE, true)).containsExactly("§aVocê se autenticou com sucesso!", "§aEnviando ao lobby...");
    }

    @Test
    void anEnglishReaderGetsTheChallengeInEnglish() {
        assertThat(prompts.challengeLines(Language.ENGLISH)).containsExactly(
            "§6§lCHALLENGE:",
            "§eType the §bcode §ein front of you §ain the chat§e!",
            BLANK,
            "§7§o(You need holograms enabled to see it properly)"
        );
    }

    @Test
    void anEnglishReaderIsToldTheEnglishCommands() {
        assertThat(prompts.credentialLines(Language.ENGLISH, false, true)).containsExactly(
            "§c§lNEVER SHARE YOUR PASSWORD WITH ANYONE!",
            BLANK,
            "§eRegister using §b/register <password> <password>",
            "§aChallenge completed successfully!"
        );
        assertThat(prompts.authenticatedLines(Language.ENGLISH, false)).containsExactly("§aYou registered successfully!", "§aSending you to the lobby...");
    }

    @Test
    void theTitleFollowsTheReadersLanguage() {
        Title portuguese = prompts.credentialsTitle(Language.PORTUGUESE, true);
        Title english = prompts.credentialsTitle(Language.ENGLISH, true);

        assertThat(portuguese.title()).isEqualTo("§b§lMUXE");
        assertThat(portuguese.subtitle()).isEqualTo("§eUse /logar <senha>");
        assertThat(english.subtitle()).isEqualTo("§eUse /login <password>");
        assertThat(english.stayTicks()).isEqualTo(20);
    }

    private static MessageCatalog defaultMessages() {
        return MessageCatalog.from(LocalizedConfig.bundled(MuxeAuthPlugin.class, "config.yml"), "messages");
    }
}
