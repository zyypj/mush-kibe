package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackSetting;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;

public final class KnockbackPage {

    public static final String ID = "knockback";

    private static final List<Entry> ENTRIES = List.of(
        new Entry(KnockbackSetting.VERTICAL, 10, "FEATHER", "Vertical", List.of("&7Valor base do impulso", "&7vertical.")),
        new Entry(KnockbackSetting.HORIZONTAL, 11, "ARROW", "Horizontal", List.of("&7Valor base do impulso", "&7horizontal.")),
        new Entry(KnockbackSetting.VERTICAL_BOOST, 12, "FIREWORK", "Vertical (Boost)", List.of("&7Valor de boost do impulso", "&7vertical.")),
        new Entry(KnockbackSetting.HORIZONTAL_BOOST, 13, "SUGAR", "Horizontal (Boost)", List.of("&7Valor de boost do impulso", "&7horizontal.")),
        new Entry(KnockbackSetting.VERTICAL_LIMIT, 14, "STEP", "Vertical máximo", List.of("&7Valor máximo do impulso", "&7vertical base.")),
        new Entry(KnockbackSetting.VERTICAL_FRICTION, 15, "WEB", "Atrito Vertical", List.of("&7Divisor do impulso vertical", "&7anterior.")),
        new Entry(KnockbackSetting.HORIZONTAL_FRICTION, 16, "SOUL_SAND", "Atrito Horizontal", List.of("&7Divisor do impulso horizontal", "&7anterior.")),
        new Entry(KnockbackSetting.ATTACKER_MULTIPLIER, 19, "IRON_SWORD", "Multiplicador de atacante", List.of("&7Multiplicador horizontal", "&7aplicado ao atacar alguém.")),
        new Entry(KnockbackSetting.RESTORE_VELOCITY, 20, "ANVIL", "Restaurar impulso", List.of("&7Se o impulso anterior", "&7é restaurado após ser", "&7enviado ao jogador.")),
        new Entry(KnockbackSetting.SIMULATE_Y, 21, "WATCH", "Simular Y", List.of("&7Faz uma simulação vertical", "&7do jogador baseado no", "&7ping antes de aplicar", "&7um impulso.")),
        new Entry(KnockbackSetting.KEEP_SPRINT, 22, "GLOWSTONE", "Manter sprint", List.of("&7Mantém o estado de sprint", "&7ao causar um impulso.")),
        new Entry(KnockbackSetting.IGNORE_SPRINT, 23, "REDSTONE", "Ignorar sprint", List.of("&7Desativa o boost especial", "&7de sprint.")),
        new Entry(KnockbackSetting.USE_ROTATION, 24, "COMPASS", "Usar rotação", List.of("&7Utiliza a rotação do", "&7atacante para causar", "&7o impulso.")),
        new Entry(KnockbackSetting.CLICK_LIMITER, 25, "STONE_BUTTON", "Limitador de cliques", List.of("&7Controla a força do", "&7limitador de cliques", "&7de knockback."))
    );

    private KnockbackPage() {
    }

    public static void write(ConfigurationSection pages, KnockbackProfile profile, String title, int backSlot, String backTo) {
        ConfigurationSection page = pages.createSection(ID);
        page.set("title", title);
        page.set("rows", 4);
        page.set("back-slot", backSlot);
        page.set("back-to", backTo);
        ConfigurationSection entries = page.createSection("entries");
        for (Entry entry : ENTRIES) {
            ConfigurationSection section = entries.createSection(entry.setting().key());
            section.set("slot", entry.slot());
            section.set("key", "knockback." + entry.setting().key());
            section.set("material", entry.material());
            section.set("name", entry.name());
            section.set("description", entry.description());
            String value = entry.setting().read(profile);
            switch (entry.setting().kind()) {
                case DECIMAL -> {
                    section.set("type", "decimal");
                    section.set("default", value);
                    section.set("min", 0);
                    section.set("max", 10);
                }
                case TOGGLE -> {
                    section.set("type", "toggle");
                    section.set("default", Boolean.parseBoolean(value));
                }
                case CHOICE -> {
                    section.set("type", "choice");
                    section.set("default", value);
                    ConfigurationSection values = section.createSection("values");
                    values.set("off.label", "Desativado");
                    values.set("low.label", "Baixo");
                    values.set("normal.label", "Normal");
                    values.set("high.label", "Alto");
                }
            }
        }
    }

    private record Entry(KnockbackSetting setting, int slot, String material, String name, List<String> description) {
    }
}
