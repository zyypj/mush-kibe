package br.com.muxe.minigame.api.customroom;

import br.com.muxe.core.common.combat.knockback.KnockbackProfile;
import br.com.muxe.core.common.combat.knockback.KnockbackSetting;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

public final class KnockbackPage {

    public static final String ID = "knockback";
    public static final String TITLE = "title";
    public static final String ENTRIES = "entries";
    public static final String VALUES = "values";
    public static final String DEFAULT_TITLE = "Alterar knockback";

    private static final List<Entry> ENTRIES_SHOWN = List.of(
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

    private static final Map<String, String> CHOICES = choices();

    private KnockbackPage() {
    }

    public static void write(ConfigurationSection pages, KnockbackProfile profile, String title, int backSlot, String backTo) {
        MemoryConfiguration texts = new MemoryConfiguration();
        texts.set(TITLE, title);
        write(pages, profile, texts, backSlot, backTo);
    }

    public static void write(ConfigurationSection pages, KnockbackProfile profile, ConfigurationSection texts, int backSlot, String backTo) {
        ConfigurationSection page = pages.createSection(ID);
        page.set("rows", 4);
        page.set("back-slot", backSlot);
        page.set("back-to", backTo);
        ConfigurationSection entries = page.createSection(ENTRIES);
        for (Entry entry : ENTRIES_SHOWN) {
            ConfigurationSection section = entries.createSection(entry.setting().key());
            section.set("slot", entry.slot());
            section.set("key", "knockback." + entry.setting().key());
            section.set("material", entry.material());
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
                }
            }
        }
        translate(pages, texts);
    }

    public static void translate(ConfigurationSection pages, ConfigurationSection texts) {
        ConfigurationSection page = pages.isConfigurationSection(ID) ? pages.getConfigurationSection(ID) : pages.createSection(ID);
        page.set(TITLE, texts.getString(TITLE, DEFAULT_TITLE));
        for (Entry entry : ENTRIES_SHOWN) {
            String key = entry.setting().key();
            String path = ENTRIES + "." + key;
            page.set(path + ".name", texts.getString(path + ".name", entry.name()));
            List<String> description = texts.getStringList(path + ".description");
            page.set(path + ".description", description.isEmpty() ? entry.description() : description);
            if (entry.setting().kind() == KnockbackSetting.Kind.CHOICE) {
                CHOICES.forEach((choice, label) -> page.set(path + "." + VALUES + "." + choice + ".label", texts.getString(VALUES + "." + choice, label)));
            }
        }
    }

    private static Map<String, String> choices() {
        Map<String, String> choices = new LinkedHashMap<>();
        choices.put("off", "Desativado");
        choices.put("low", "Baixo");
        choices.put("normal", "Normal");
        choices.put("high", "Alto");
        return choices;
    }

    private record Entry(KnockbackSetting setting, int slot, String material, String name, List<String> description) {
    }
}
