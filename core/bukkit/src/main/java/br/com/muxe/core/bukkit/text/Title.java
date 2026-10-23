package br.com.muxe.core.bukkit.text;

import br.com.muxe.core.bukkit.nms.LegacyPackets;
import org.bukkit.entity.Player;

public record Title(String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {

    public static final int DEFAULT_FADE_IN = 10;
    public static final int DEFAULT_STAY = 70;
    public static final int DEFAULT_FADE_OUT = 20;

    public Title {
        title = Colors.translate(title);
        subtitle = Colors.translate(subtitle);
        if (fadeInTicks < 0 || stayTicks < 0 || fadeOutTicks < 0) {
            throw new IllegalArgumentException("Title timings cannot be negative");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Title of(String title, String subtitle) {
        return builder().title(title).subtitle(subtitle).build();
    }

    @SuppressWarnings("deprecation")
    public void send(Player player) {
        if (!LegacyPackets.sendTitle(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks)) {
            player.sendTitle(title, subtitle);
        }
    }

    @SuppressWarnings("deprecation")
    public static void clear(Player player) {
        player.resetTitle();
    }

    public static final class Builder {

        private String title = "";
        private String subtitle = "";
        private int fadeIn = DEFAULT_FADE_IN;
        private int stay = DEFAULT_STAY;
        private int fadeOut = DEFAULT_FADE_OUT;

        private Builder() {
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder subtitle(String value) {
            this.subtitle = value;
            return this;
        }

        public Builder fadeIn(int ticks) {
            this.fadeIn = ticks;
            return this;
        }

        public Builder stay(int ticks) {
            this.stay = ticks;
            return this;
        }

        public Builder fadeOut(int ticks) {
            this.fadeOut = ticks;
            return this;
        }

        public Builder timings(int fadeInTicks, int stayTicks, int fadeOutTicks) {
            return fadeIn(fadeInTicks).stay(stayTicks).fadeOut(fadeOutTicks);
        }

        public Title build() {
            return new Title(title, subtitle, fadeIn, stay, fadeOut);
        }
    }
}
