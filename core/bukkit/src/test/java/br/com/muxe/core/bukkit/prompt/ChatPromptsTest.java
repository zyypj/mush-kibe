package br.com.muxe.core.bukkit.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatPromptsTest {

    @Test
    void thePortugueseAndTheEnglishWordBothCancel() {
        assertThat(ChatPrompts.cancels("cancelar")).isTrue();
        assertThat(ChatPrompts.cancels(" Cancel ")).isTrue();
        assertThat(ChatPrompts.cancels("MUXE10")).isFalse();
    }
}
