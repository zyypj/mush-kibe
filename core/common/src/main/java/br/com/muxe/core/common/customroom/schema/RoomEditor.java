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
import java.util.Locale;
import java.util.Optional;

public final class RoomEditor {

    public static final String TEAM_COUNT = "teams.count";
    public static final int TIMER_STEP = 30;
    public static final int TIMER_SHIFT_STEP = 60;
    public static final int MAX_TIMER_SECONDS = 3600;
    public static final int MAX_PRICE = 999;
    public static final int MAX_LIMIT = 999;
    public static final int MAX_NAME = 16;

    private final RoomSchema schema;
    private final ModeContext context;

    public RoomEditor(RoomSchema schema, ModeContext context) {
        this.schema = schema;
        this.context = context;
    }

    public RoomSchema schema() {
        return schema;
    }

    public ModeContext context() {
        return context;
    }

    public double stepper(Stepper entry, RoomOptions options) {
        double fallback = context.resolveDecimal(entry.defaultValue(), 1);
        return clamp(options.decimal(entry.key(), fallback), min(entry), max(entry));
    }

    public double min(Stepper entry) {
        return context.resolveDecimal(entry.min(), 0);
    }

    public double max(Stepper entry) {
        return Math.max(min(entry), context.resolveDecimal(entry.max(), Integer.MAX_VALUE));
    }

    public boolean toggle(Toggle entry, RoomOptions options) {
        return options.flag(entry.key(), entry.defaultValue());
    }

    public String decimal(Decimal entry, RoomOptions options) {
        return options.get(entry.key()).orElse(entry.defaultValue());
    }

    public ChoiceValue choice(Choice entry, RoomOptions options) {
        String chosen = options.get(entry.key(), entry.defaultValue());
        return entry.values().stream().filter(value -> value.id().equals(chosen)).findFirst()
            .orElseGet(() -> entry.values().stream().filter(value -> value.id().equals(entry.defaultValue())).findFirst().orElse(entry.values().getFirst()));
    }

    public int timerSeconds(Timer entry, RoomOptions options) {
        return Math.max(0, options.integer(entry.key() + ".seconds", entry.defaultSeconds()));
    }

    public boolean timerEnabled(Timer entry, RoomOptions options) {
        return options.flag(entry.key() + ".enabled", true);
    }

    public int teamCount(RoomOptions options) {
        return schema.entries(Stepper.class).stream().filter(entry -> entry.key().equals(TEAM_COUNT)).findFirst()
            .map(entry -> (int) stepper(entry, options))
            .orElse(context.teams());
    }

    public boolean teamEnabled(int index, RoomOptions options) {
        return index <= teamCount(options) && options.flag(teamKey(index, "enabled"), true);
    }

    public Optional<String> teamName(int index, RoomOptions options) {
        return options.get(teamKey(index, "name"));
    }

    public boolean free(String scope, RoomOptions options) {
        return options.flag(scope + ".free", false);
    }

    public ShopState shop(ShopItem entry, RoomOptions options) {
        String prefix = "shop." + entry.key() + ".";
        boolean enabled = options.flag(prefix + "enabled", entry.enabled());
        int price = Math.max(0, options.integer(prefix + "price", entry.price()));
        String currency = options.get(prefix + "currency").filter(id -> schema.currency(id).isPresent()).orElse(entry.currency());
        LimitType limitType = LimitType.parse(options.get(prefix + "limit-type", LimitType.NONE.key()));
        int limit = Math.max(1, options.integer(prefix + "limit", 1));
        return new ShopState(enabled, price, currency, limitType, limit, free("shop", options),
            options.has(prefix + "enabled"), options.has(prefix + "price") || options.has(prefix + "currency"),
            options.has(prefix + "limit-type") || options.has(prefix + "limit"));
    }

    public boolean upgradeEnabled(Upgrade entry, RoomOptions options) {
        return options.flag("upgrades." + entry.key() + ".enabled", entry.enabled());
    }

    public int levelPrice(Upgrade entry, int level, RoomOptions options) {
        return Math.max(0, options.integer(levelKey(entry, level, "price"), entry.levels().get(level - 1).price()));
    }

    public String levelCurrency(Upgrade entry, int level, RoomOptions options) {
        return options.get(levelKey(entry, level, "currency")).filter(id -> schema.currency(id).isPresent())
            .orElse(entry.levels().get(level - 1).currency());
    }

    public boolean levelChanged(Upgrade entry, int level, RoomOptions options) {
        return options.has(levelKey(entry, level, "price")) || options.has(levelKey(entry, level, "currency"));
    }

    public boolean upgradeChanged(Upgrade entry, RoomOptions options) {
        return options.has("upgrades." + entry.key() + ".enabled") || !options.startingWith("upgrades." + entry.key() + ".").isEmpty()
            || free("upgrades", options);
    }

    public Outcome click(OptionEntry entry, Click click, RoomOptions options) {
        return switch (entry) {
            case Link link -> new Outcome.Open(link.page());
            case Stepper stepper -> step(stepper, click, options);
            case Toggle toggle -> changed(set(options, toggle.key(), String.valueOf(!toggle(toggle, options)), String.valueOf(toggle.defaultValue())));
            case Decimal decimal -> new Outcome.Ask(new Prompt(decimal.key(), PromptKind.DECIMAL, decimal(decimal, options), decimal.name(), decimal.min(), decimal.max()));
            case Choice choice -> changed(set(options, choice.key(), next(choice, options).id(), choice.defaultValue()));
            case Timer timer -> time(timer, click, options);
            case Team team -> team(team, click, options);
            case ShopItem item -> new Outcome.EditShop(item);
            case Upgrade upgrade -> new Outcome.EditUpgrade(upgrade);
            case FreeAll freeAll -> changed(set(options, freeAll.scope() + ".free", String.valueOf(!free(freeAll.scope(), options)), "false"));
        };
    }

    public Outcome shopClick(ShopItem entry, ShopField field, Click click, RoomOptions options) {
        String prefix = "shop." + entry.key() + ".";
        ShopState state = shop(entry, options);
        return switch (field) {
            case STATUS -> changed(set(options, prefix + "enabled", String.valueOf(!state.enabled()), String.valueOf(entry.enabled())));
            case PRICE -> click.isRight()
                ? changed(set(options, prefix + "currency", schema.nextCurrency(state.currency()), entry.currency()))
                : new Outcome.Ask(new Prompt(prefix + "price", PromptKind.INTEGER, String.valueOf(state.price()), entry.name(), 0, MAX_PRICE));
            case LIMIT -> {
                if (click.isRight()) {
                    yield changed(set(options, prefix + "limit-type", state.limitType().next().key(), LimitType.NONE.key()));
                }
                if (state.limitType() == LimitType.NONE) {
                    yield Outcome.NOTHING;
                }
                yield new Outcome.Ask(new Prompt(prefix + "limit", PromptKind.INTEGER, String.valueOf(state.limit()), entry.name(), 1, MAX_LIMIT));
            }
        };
    }

    public Outcome upgradeClick(Upgrade entry, int level, Click click, RoomOptions options) {
        if (level <= 0) {
            boolean enabled = upgradeEnabled(entry, options);
            return changed(set(options, "upgrades." + entry.key() + ".enabled", String.valueOf(!enabled), String.valueOf(entry.enabled())));
        }
        if (level > entry.levels().size()) {
            return Outcome.NOTHING;
        }
        if (click.isRight()) {
            return changed(set(options, levelKey(entry, level, "currency"), schema.nextCurrency(levelCurrency(entry, level, options)),
                entry.levels().get(level - 1).currency()));
        }
        return new Outcome.Ask(new Prompt(levelKey(entry, level, "price"), PromptKind.INTEGER, String.valueOf(levelPrice(entry, level, options)),
            entry.name(), 0, MAX_PRICE));
    }

    public Optional<RoomOptions> answer(Prompt prompt, String typed, RoomOptions options) {
        String text = typed.trim();
        return switch (prompt.kind()) {
            case DECIMAL -> parseDecimal(text).filter(value -> value >= prompt.min() && value <= prompt.max())
                .map(value -> set(options, prompt.key(), Double.toString(value), defaultOf(prompt.key())));
            case INTEGER -> parseInteger(text).filter(value -> value >= prompt.min() && value <= prompt.max())
                .map(value -> set(options, prompt.key(), String.valueOf(value), defaultOf(prompt.key())));
            case TEXT -> text.isEmpty() || text.length() > MAX_NAME ? Optional.empty() : Optional.of(options.with(prompt.key(), text));
        };
    }

    public static String format(Stepper entry, double value) {
        return switch (entry.format()) {
            case INTEGER -> String.valueOf(Math.round(value));
            case SECONDS -> Math.round(value) + "s";
            case MULTIPLIER -> trim(value).replace('.', ',') + "x";
        };
    }

    public static String clock(int seconds) {
        return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
    }

    public static String teamKey(int index, String field) {
        return "teams." + index + "." + field;
    }

    private Outcome step(Stepper entry, Click click, RoomOptions options) {
        double current = stepper(entry, options);
        double changed = clamp(click.isRight() ? current - entry.step() : current + entry.step(), min(entry), max(entry));
        if (changed == current) {
            return Outcome.NOTHING;
        }
        double fallback = context.resolveDecimal(entry.defaultValue(), 1);
        return changed(set(options, entry.key(), trim(changed), trim(fallback)));
    }

    private Outcome time(Timer entry, Click click, RoomOptions options) {
        if (click == Click.DROP) {
            if (!entry.toggleable()) {
                return Outcome.NOTHING;
            }
            return changed(set(options, entry.key() + ".enabled", String.valueOf(!timerEnabled(entry, options)), "true"));
        }
        int step = click.isShift() ? TIMER_SHIFT_STEP : TIMER_STEP;
        int seconds = timerSeconds(entry, options) + (click.isRight() ? -step : step);
        seconds = Math.max(0, Math.min(MAX_TIMER_SECONDS, seconds));
        return changed(set(options, entry.key() + ".seconds", String.valueOf(seconds), String.valueOf(entry.defaultSeconds())));
    }

    private Outcome team(Team entry, Click click, RoomOptions options) {
        String nameKey = teamKey(entry.index(), "name");
        return switch (click) {
            case DROP -> options.has(nameKey) ? changed(options.without(nameKey)) : Outcome.NOTHING;
            case RIGHT, SHIFT_RIGHT -> new Outcome.Ask(new Prompt(nameKey, PromptKind.TEXT, options.get(nameKey, ""), "", 0, 0));
            case LEFT, SHIFT_LEFT -> changed(set(options, teamKey(entry.index(), "enabled"),
                String.valueOf(!options.flag(teamKey(entry.index(), "enabled"), true)), "true"));
        };
    }

    private ChoiceValue next(Choice entry, RoomOptions options) {
        int index = entry.values().indexOf(choice(entry, options));
        return entry.values().get((index + 1) % entry.values().size());
    }

    private String defaultOf(String key) {
        for (OptionEntry entry : schema.entries()) {
            switch (entry) {
                case Decimal decimal when decimal.key().equals(key) -> {
                    return Double.toString(Double.parseDouble(decimal.defaultValue()));
                }
                case ShopItem item when key.equals("shop." + item.key() + ".price") -> {
                    return String.valueOf(item.price());
                }
                case ShopItem item when key.equals("shop." + item.key() + ".limit") -> {
                    return "1";
                }
                case Upgrade upgrade when key.startsWith("upgrades." + upgrade.key() + ".") && key.endsWith(".price") -> {
                    String level = key.substring(("upgrades." + upgrade.key() + ".").length(), key.length() - ".price".length());
                    return parseInteger(level).filter(number -> number >= 1 && number <= upgrade.levels().size())
                        .map(number -> String.valueOf(upgrade.levels().get(number - 1).price())).orElse(null);
                }
                default -> {
                }
            }
        }
        return null;
    }

    private static RoomOptions set(RoomOptions options, String key, String value, String fallback) {
        return value.equals(fallback) ? options.without(key) : options.with(key, value);
    }

    private static Outcome changed(RoomOptions options) {
        return new Outcome.Changed(options);
    }

    private static String levelKey(Upgrade entry, int level, String field) {
        return "upgrades." + entry.key() + "." + level + "." + field;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.ROOT, "%.4f", value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static Optional<Double> parseDecimal(String text) {
        try {
            double value = Double.parseDouble(text.replace(',', '.'));
            return Double.isFinite(value) ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInteger(String text) {
        try {
            return Optional.of(Integer.parseInt(text));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    public enum Click {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        DROP;

        public boolean isRight() {
            return this == RIGHT || this == SHIFT_RIGHT;
        }

        public boolean isShift() {
            return this == SHIFT_LEFT || this == SHIFT_RIGHT;
        }
    }

    public enum ShopField {
        STATUS,
        PRICE,
        LIMIT
    }

    public enum PromptKind {
        DECIMAL,
        INTEGER,
        TEXT
    }

    public enum LimitType {
        NONE("none"),
        TEAM("team"),
        PLAYER("player"),
        INVENTORY("inventory");

        private final String key;

        LimitType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public LimitType next() {
            LimitType[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public static LimitType parse(String key) {
            for (LimitType type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    public record Prompt(String key, PromptKind kind, String current, String label, double min, double max) {
    }

    public record ShopState(boolean enabled, int price, String currency, LimitType limitType, int limit, boolean free, boolean statusChanged,
                            boolean priceChanged, boolean limitChanged) {

        public boolean changed() {
            return statusChanged || priceChanged || limitChanged || free;
        }
    }

    public sealed interface Outcome {

        Outcome NOTHING = new Nothing();

        record Changed(RoomOptions options) implements Outcome {
        }

        record Open(String page) implements Outcome {
        }

        record EditShop(ShopItem item) implements Outcome {
        }

        record EditUpgrade(Upgrade upgrade) implements Outcome {
        }

        record Ask(Prompt prompt) implements Outcome {
        }

        record Nothing() implements Outcome {
        }
    }
}
