package br.com.muxe.core.bukkit.language;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.language.menu.LanguageMenu;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.bukkit.entity.Player;

@Command(name = "idioma", aliases = {"language", "lang", "linguagem"}, description = "Changes the language of the server messages.", usage = "language-usage")
public final class LanguageCommand {

    private final PlayerLanguages languages;
    private final MessageCatalog messages;
    private final LanguageMenu menu;
    private final Executor mainThread;

    public LanguageCommand(PlayerLanguages languages, MessageCatalog messages, LanguageMenu menu, Executor mainThread) {
        this.languages = languages;
        this.messages = messages;
        this.menu = menu;
        this.mainThread = mainThread;
    }

    @Default
    public void change(Player player, @Arg("idioma") @Suggest({"português", "inglês", "portuguese", "english"}) Optional<String> choice) {
        if (choice.isEmpty()) {
            menu.build(languages.of(player), this::pick).open(player);
            return;
        }
        Optional<Language> wanted = choice.flatMap(Language::named);
        if (wanted.isEmpty()) {
            player.sendMessage(messages.get(player, "language-usage", "language", name(languages.of(player))));
            return;
        }
        pick(player, wanted.get());
    }

    private void pick(Player player, Language language) {
        if (languages.of(player) == language) {
            player.sendMessage(messages.get(player, "language-already", "language", name(language)));
            return;
        }
        languages.change(player, language).whenComplete((result, error) -> mainThread.execute(() -> {
            if (!player.isOnline()) {
                return;
            }
            if (error != null || result == null || !result.isSuccess()) {
                player.sendMessage(messages.get(player, "language-failed"));
                return;
            }
            player.sendMessage(messages.get(language, "language-changed", "language", name(language)));
        }));
    }

    private Object name(Language language) {
        return messages.text("language-name-" + language.tag().toLowerCase(Locale.ROOT));
    }
}
