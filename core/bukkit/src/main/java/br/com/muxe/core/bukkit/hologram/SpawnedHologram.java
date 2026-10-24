package br.com.muxe.core.bukkit.hologram;

public interface SpawnedHologram {

    SpawnedHologram NONE = new SpawnedHologram() {
        @Override
        public void resend() {
        }

        @Override
        public void remove() {
        }
    };

    void resend();

    void remove();
}
