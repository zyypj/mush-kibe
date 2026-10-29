package br.com.muxe.core.bukkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandTextTest {

    @Test
    void theEmptyLineTokenBecomesABlankLine() {
        assertThat(CommandText.line("{VAZIO}")).isEmpty();
        assertThat(CommandText.line("&b&lRANK ENDERLORE")).isEqualTo("&b&lRANK ENDERLORE");
    }

    @Test
    void idsAreLowercase() {
        assertThat(CommandText.id("Caixa")).isEqualTo("caixa");
    }

    @Test
    void lineNumbersArePositive() {
        assertThat(CommandText.number("2")).hasValue(2);
        assertThat(CommandText.number("0")).isEmpty();
        assertThat(CommandText.number("dois")).isEmpty();
    }
}
