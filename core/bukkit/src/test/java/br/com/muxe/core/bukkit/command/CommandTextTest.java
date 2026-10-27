package br.com.muxe.core.bukkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommandTextTest {

    private static final String[] ARGS = {"Linha", "caixa", "2", "&b&lRANK", "ENDERLORE"};

    @Test
    void textIsJoinedFromAnArgument() {
        assertThat(CommandText.join(ARGS, 3)).isEqualTo("&b&lRANK ENDERLORE");
        assertThat(CommandText.join(ARGS, 9)).isEmpty();
    }

    @Test
    void theEmptyLineTokenBecomesABlankLine() {
        assertThat(CommandText.line(new String[] {"adicionar", "caixa", "{VAZIO}"}, 2)).isEmpty();
        assertThat(CommandText.line(ARGS, 3)).isEqualTo("&b&lRANK ENDERLORE");
    }

    @Test
    void wordsAreLowercaseAndMissingWordsAreEmpty() {
        assertThat(CommandText.word(ARGS, 0)).isEqualTo("linha");
        assertThat(CommandText.word(ARGS, 7)).isEmpty();
    }

    @Test
    void lineNumbersArePositive() {
        assertThat(CommandText.number("2")).hasValue(2);
        assertThat(CommandText.number("0")).isEmpty();
        assertThat(CommandText.number("dois")).isEmpty();
    }
}
