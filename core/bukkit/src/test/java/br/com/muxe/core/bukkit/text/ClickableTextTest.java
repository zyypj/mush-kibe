package br.com.muxe.core.bukkit.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.junit.jupiter.api.Test;

class ClickableTextTest {

    @Test
    void eachButtonKeepsItsColoursAndClickAction() {
        BaseComponent[] line = ClickableText.line(List.of(
            ChatButton.text("&a✔ &fSpawn "),
            ChatButton.run("&e[Definir]", "/arena definir spawn azul", "&7Clique para definir."),
            ChatButton.suggest(" &b[Nome]", "/arena nome ")
        ));

        assertThat(TextComponent.toLegacyText(line)).isEqualTo("§a✔ §fSpawn §e[Definir]§f §b[Nome]");
        assertThat(line[0].getClickEvent()).isNull();
        BaseComponent define = componentWithText(line, "[Definir]");
        assertThat(define.getClickEvent().getAction()).isEqualTo(ClickEvent.Action.RUN_COMMAND);
        assertThat(define.getClickEvent().getValue()).isEqualTo("/arena definir spawn azul");
        assertThat(TextComponent.toLegacyText(define.getHoverEvent().getValue())).isEqualTo("§7Clique para definir.");
        BaseComponent name = componentWithText(line, "[Nome]");
        assertThat(name.getClickEvent().getAction()).isEqualTo(ClickEvent.Action.SUGGEST_COMMAND);
        assertThat(name.getHoverEvent()).isNull();
    }

    @Test
    void aClickableButtonNeedsASlashCommand() {
        assertThatThrownBy(() -> ChatButton.run("&e[Ir]", "arena ir")).isInstanceOf(IllegalArgumentException.class);
    }

    private static BaseComponent componentWithText(BaseComponent[] line, String text) {
        for (BaseComponent component : line) {
            if (component.toPlainText().contains(text)) {
                return component;
            }
        }
        throw new AssertionError("No component with " + text);
    }
}
