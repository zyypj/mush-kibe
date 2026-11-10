package br.com.muxe.core.common.customroom.schema;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Choice;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ChoiceValue;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Decimal;
import br.com.muxe.core.common.customroom.schema.OptionEntry.FreeAll;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Link;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ShopItem;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Stepper;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Team;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Timer;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Toggle;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Upgrade;
import br.com.muxe.core.common.customroom.schema.RoomEditor.LimitType;
import br.com.muxe.core.common.customroom.schema.RoomEditor.ShopState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RoomLore {

    public static final int SHOP_STATUS_SLOT = 11;
    public static final int SHOP_PRICE_SLOT = 13;
    public static final int SHOP_LIMIT_SLOT = 15;
    public static final int SHOP_BACK_SLOT = 21;
    public static final int UPGRADE_STATUS_SLOT = 10;
    public static final int UPGRADE_FIRST_LEVEL_SLOT = 12;
    public static final int UPGRADE_BACK_SLOT = 22;
    public static final Icon TEAM_ICON = Icon.of("BANNER");
    public static final Icon TEAM_OFF_ICON = Icon.of("BARRIER");
    public static final Icon DISABLED_ICON = Icon.of("BARRIER");
    public static final Icon STATUS_ON_ICON = Icon.of("INK_SACK", 10);
    public static final Icon STATUS_OFF_ICON = Icon.of("INK_SACK", 8);
    public static final Icon LIMIT_ICON = Icon.of("IRON_DOOR");

    private final RoomEditor editor;
    private final RoomTexts texts;

    public RoomLore(RoomEditor editor, RoomTexts texts) {
        this.editor = editor;
        this.texts = texts;
    }

    public RoomEditor editor() {
        return editor;
    }

    public RoomTexts texts() {
        return texts;
    }

    public Shown entry(OptionEntry entry, RoomOptions options) {
        return switch (entry) {
            case Link link -> new Shown(link.icon(), "&a" + link.name(), link.lore());
            case Stepper stepper -> stepper(stepper, options);
            case Toggle toggle -> toggle(toggle, options);
            case Decimal decimal -> decimal(decimal, options);
            case Choice choice -> choice(choice, options);
            case Timer timer -> timer(timer, options);
            case Team team -> team(team, options);
            case ShopItem item -> shopItem(item, options);
            case Upgrade upgrade -> upgrade(upgrade, options);
            case FreeAll freeAll -> freeAll(freeAll, options);
        };
    }

    public Map<Integer, Shown> shopEditor(ShopItem item, RoomOptions options) {
        ShopState state = editor.shop(item, options);
        Map<Integer, Shown> shown = new LinkedHashMap<>();
        shown.put(SHOP_STATUS_SLOT, status(state.enabled(), state.statusChanged()));
        Currency currency = editor.schema().currencyOrFirst(state.currency());
        List<String> priceLore = List.of(texts.get("blank"), texts.get("value-plain", "value", price(state.price(), currency, state.free()) + mark(state.priceChanged() || state.free())),
            texts.get("blank"), texts.get("change-value"), texts.get("change-currency"));
        shown.put(SHOP_PRICE_SLOT, new Shown(currency.icon().withAmount(state.price()).withGlow(state.priceChanged() || state.free()), texts.get("price-name"), priceLore));
        List<String> limitLore = new ArrayList<>();
        limitLore.add(texts.get("blank"));
        limitLore.add(texts.get("value-plain", "value", "&f" + limit(state.limitType(), state.limit()) + mark(state.limitChanged())));
        limitLore.add(texts.get("blank"));
        if (state.limitType() != LimitType.NONE) {
            limitLore.add(texts.get("change-amount"));
        }
        limitLore.add(texts.get("change-type"));
        shown.put(SHOP_LIMIT_SLOT, new Shown(LIMIT_ICON.withGlow(state.limitChanged()), texts.get("limit-name"), limitLore));
        return shown;
    }

    public Map<Integer, Shown> upgradeEditor(Upgrade upgrade, RoomOptions options) {
        Map<Integer, Shown> shown = new LinkedHashMap<>();
        boolean enabled = editor.upgradeEnabled(upgrade, options);
        shown.put(UPGRADE_STATUS_SLOT, status(enabled, options.has("upgrades." + upgrade.key() + ".enabled")));
        boolean free = editor.free("upgrades", options);
        for (int level = 1; level <= upgrade.levels().size(); level++) {
            Currency currency = editor.schema().currencyOrFirst(editor.levelCurrency(upgrade, level, options));
            int price = editor.levelPrice(upgrade, level, options);
            boolean changed = editor.levelChanged(upgrade, level, options) || free;
            List<String> lore = new ArrayList<>();
            lore.add(texts.get("blank"));
            String label = upgrade.levels().get(level - 1).label();
            if (!label.isBlank()) {
                lore.add(texts.get("level-label", "label", label));
                lore.add(texts.get("blank"));
            }
            lore.add(texts.get("price", "price", price(price, currency, free) + mark(changed)));
            lore.add(texts.get("blank"));
            lore.add(texts.get("change-value"));
            lore.add(texts.get("change-currency"));
            shown.put(UPGRADE_FIRST_LEVEL_SLOT + level - 1, new Shown(currency.icon().withAmount(price).withGlow(changed),
                texts.get("level-name", "level", RoomTexts.roman(level)), lore));
        }
        return shown;
    }

    public String teamName(int index, RoomOptions options) {
        return editor.teamName(index, options).orElseGet(() -> texts.get("team-name", "index", String.valueOf(index)));
    }

    private Shown stepper(Stepper entry, RoomOptions options) {
        double value = editor.stepper(entry, options);
        List<String> lore = new ArrayList<>(entry.description());
        lore.add(texts.get("blank"));
        lore.add(texts.get("value", "value", RoomEditor.format(entry, value)));
        lore.add(texts.get("blank"));
        lore.add(texts.get("left-increase"));
        lore.add(texts.get("right-decrease"));
        Icon icon = entry.amountFromValue() ? entry.icon().withAmount((int) Math.round(value)) : entry.icon();
        return new Shown(icon, "&a" + entry.name(), lore);
    }

    private Shown toggle(Toggle entry, RoomOptions options) {
        boolean on = editor.toggle(entry, options);
        List<String> lore = new ArrayList<>(entry.description());
        lore.add(texts.get("blank"));
        lore.add(texts.get("value-plain", "value", texts.get(on ? "enabled" : "disabled")));
        lore.add(texts.get("blank"));
        lore.add(texts.get(on ? "click-disable" : "click-enable"));
        return new Shown(entry.icon().withGlow(on), (on ? "&a" : "&c") + entry.name(), lore);
    }

    private Shown decimal(Decimal entry, RoomOptions options) {
        List<String> lore = new ArrayList<>(entry.description());
        lore.add(texts.get("blank"));
        lore.add(texts.get("value", "value", editor.decimal(entry, options)));
        lore.add(texts.get("blank"));
        lore.add(texts.get("click-change"));
        return new Shown(entry.icon(), "&a" + entry.name(), lore);
    }

    private Shown choice(Choice entry, RoomOptions options) {
        ChoiceValue chosen = editor.choice(entry, options);
        List<String> lore = new ArrayList<>(entry.description());
        lore.add(texts.get("blank"));
        for (ChoiceValue value : entry.values()) {
            lore.add(texts.get(value.equals(chosen) ? "choice-selected" : "choice-other", "label", value.label()));
        }
        lore.add(texts.get("blank"));
        lore.add(texts.get("click-switch"));
        return new Shown(chosen.icon(), "&a" + entry.name(), lore);
    }

    private Shown timer(Timer entry, RoomOptions options) {
        boolean on = editor.timerEnabled(entry, options);
        List<String> lore = new ArrayList<>();
        lore.add(texts.get("blank"));
        lore.add(texts.get("time", "time", RoomEditor.clock(editor.timerSeconds(entry, options))));
        lore.add(texts.get("status", "status", texts.get(on ? "enabled" : "disabled")));
        lore.add(texts.get("blank"));
        lore.add(texts.get("timer-left"));
        lore.add(texts.get("timer-right"));
        lore.add(texts.get("timer-shift"));
        if (entry.toggleable()) {
            lore.add(texts.get("timer-drop"));
        }
        return new Shown(entry.icon(), (on ? "&a" : "&c") + entry.name(), lore);
    }

    private Shown team(Team entry, RoomOptions options) {
        boolean enabled = options.flag(RoomEditor.teamKey(entry.index(), "enabled"), true);
        boolean renamed = editor.teamName(entry.index(), options).isPresent();
        List<String> lore = new ArrayList<>();
        lore.add(texts.get("blank"));
        if (renamed) {
            lore.add(texts.get("team-original", "name", texts.get("team-name", "index", String.valueOf(entry.index()))));
        }
        lore.add(texts.get("status", "status", texts.get(enabled ? "enabled" : "disabled")));
        lore.add(texts.get("blank"));
        lore.add(texts.get("team-toggle"));
        lore.add(texts.get("team-rename"));
        if (renamed) {
            lore.add(texts.get("team-restore"));
        }
        Icon icon = (enabled ? TEAM_ICON : TEAM_OFF_ICON).withGlow(renamed || !enabled);
        return new Shown(icon, (enabled ? "&f" : "&c") + teamName(entry.index(), options), lore);
    }

    private Shown shopItem(ShopItem entry, RoomOptions options) {
        ShopState state = editor.shop(entry, options);
        Currency currency = editor.schema().currencyOrFirst(state.currency());
        List<String> lore = new ArrayList<>();
        lore.add(texts.get("blank"));
        if (!entry.description().isEmpty()) {
            lore.addAll(entry.description());
            lore.add(texts.get("blank"));
        }
        lore.add(texts.get("status", "status", texts.get(state.enabled() ? "enabled" : "disabled") + mark(state.statusChanged())));
        lore.add(texts.get("price", "price", price(state.price(), currency, state.free()) + mark(state.priceChanged() || state.free())));
        lore.add(texts.get("limit", "limit", limit(state.limitType(), state.limit()) + mark(state.limitChanged())));
        lore.add(texts.get("blank"));
        lore.add(texts.get("click-change"));
        return new Shown(entry.icon().withGlow(state.changed()), "&a" + entry.name(), lore);
    }

    private Shown upgrade(Upgrade entry, RoomOptions options) {
        boolean enabled = editor.upgradeEnabled(entry, options);
        boolean free = editor.free("upgrades", options);
        List<String> lore = new ArrayList<>();
        lore.add(texts.get("blank"));
        lore.addAll(entry.description());
        lore.add(texts.get("blank"));
        lore.add(texts.get("status", "status", texts.get(enabled ? "enabled" : "disabled")));
        for (int level = 1; level <= entry.levels().size(); level++) {
            Currency currency = editor.schema().currencyOrFirst(editor.levelCurrency(entry, level, options));
            boolean changed = editor.levelChanged(entry, level, options) || free;
            lore.add(texts.get("level-line", "level", RoomTexts.roman(level), "price", price(editor.levelPrice(entry, level, options), currency, free) + mark(changed)));
        }
        lore.add(texts.get("blank"));
        lore.add(texts.get("click-change"));
        Icon icon = enabled ? entry.icon() : DISABLED_ICON;
        return new Shown(icon.withGlow(editor.upgradeChanged(entry, options)), "&a" + entry.name(), lore);
    }

    private Shown freeAll(FreeAll entry, RoomOptions options) {
        boolean on = editor.free(entry.scope(), options);
        List<String> lore = List.of(texts.get("blank"), entry.description(), texts.get("blank"), texts.get(on ? "free-on-click" : "free-off-click"));
        return new Shown(Icon.of("GOLD_NUGGET").withGlow(on), texts.get(on ? "free-on-name" : "free-off-name"), lore);
    }

    private Shown status(boolean enabled, boolean changed) {
        List<String> lore = List.of(texts.get("blank"), texts.get("value-plain", "value", texts.get(enabled ? "enabled" : "disabled") + mark(changed)),
            texts.get("blank"), texts.get(enabled ? "click-disable" : "click-enable"));
        return new Shown((enabled ? STATUS_ON_ICON : STATUS_OFF_ICON).withGlow(changed), texts.get("status-name"), lore);
    }

    private String price(int amount, Currency currency, boolean free) {
        return free || amount == 0 ? texts.get("free") : currency.amount(amount);
    }

    private String limit(LimitType type, int amount) {
        return switch (type) {
            case NONE -> texts.get("limit-none");
            case TEAM -> texts.get("limit-team", "amount", String.valueOf(amount));
            case PLAYER -> texts.get("limit-player", "amount", String.valueOf(amount));
            case INVENTORY -> texts.get("limit-inventory", "amount", String.valueOf(amount));
        };
    }

    private String mark(boolean changed) {
        return changed ? texts.get("changed") : "";
    }

    public record Shown(Icon icon, String name, List<String> lore) {

        public Shown {
            lore = List.copyOf(lore);
        }
    }
}
