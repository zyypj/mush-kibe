package br.com.muxe.core.bukkit.credit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CreditFormatTest {

    @Test
    void thousandsAreSeparatedWithDots() {
        assertThat(CreditFormat.format(0)).isEqualTo("0");
        assertThat(CreditFormat.format(750)).isEqualTo("750");
        assertThat(CreditFormat.format(1_750)).isEqualTo("1.750");
        assertThat(CreditFormat.format(1_234_567)).isEqualTo("1.234.567");
        assertThat(CreditFormat.format(-2_000)).isEqualTo("-2.000");
    }
}
