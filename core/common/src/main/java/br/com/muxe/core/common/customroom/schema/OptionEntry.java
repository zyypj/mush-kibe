package br.com.muxe.core.common.customroom.schema;

import java.util.List;
import java.util.Objects;

public sealed interface OptionEntry {

    int slot();

    record Link(int slot, Icon icon, String name, List<String> lore, String page) implements OptionEntry {

        public Link {
            Objects.requireNonNull(page, "page");
            lore = List.copyOf(lore);
        }
    }

    record Stepper(int slot, Icon icon, String name, List<String> description, String key, String defaultValue, String min, String max, double step,
                   Format format, boolean amountFromValue) implements OptionEntry {

        public Stepper {
            Objects.requireNonNull(key, "key");
            description = List.copyOf(description);
            if (!(step > 0)) {
                throw new IllegalArgumentException("A stepper needs a positive step, got " + step);
            }
        }
    }

    record Toggle(int slot, Icon icon, String name, List<String> description, String key, boolean defaultValue) implements OptionEntry {

        public Toggle {
            Objects.requireNonNull(key, "key");
            description = List.copyOf(description);
        }
    }

    record Decimal(int slot, Icon icon, String name, List<String> description, String key, String defaultValue, double min, double max) implements OptionEntry {

        public Decimal {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(defaultValue, "defaultValue");
            description = List.copyOf(description);
        }
    }

    record Choice(int slot, String name, List<String> description, String key, List<ChoiceValue> values, String defaultValue) implements OptionEntry {

        public Choice {
            Objects.requireNonNull(key, "key");
            description = List.copyOf(description);
            values = List.copyOf(values);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("A choice needs at least one value: " + key);
            }
        }
    }

    record ChoiceValue(String id, String label, Icon icon) {
    }

    record Timer(int slot, Icon icon, String name, String key, int defaultSeconds, boolean toggleable) implements OptionEntry {

        public Timer {
            Objects.requireNonNull(key, "key");
        }
    }

    record Team(int slot, int index) implements OptionEntry {
    }

    record ShopItem(int slot, String key, Icon icon, String name, List<String> description, int price, String currency) implements OptionEntry {

        public ShopItem {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(currency, "currency");
            description = List.copyOf(description);
        }
    }

    record Upgrade(int slot, String key, Icon icon, String name, List<String> description, List<Level> levels, boolean enabled) implements OptionEntry {

        public Upgrade {
            Objects.requireNonNull(key, "key");
            description = List.copyOf(description);
            levels = List.copyOf(levels);
        }
    }

    record Level(String label, int price, String currency) {
    }

    record FreeAll(int slot, String scope, String description) implements OptionEntry {

        public FreeAll {
            Objects.requireNonNull(scope, "scope");
        }
    }

    enum Format {
        INTEGER,
        MULTIPLIER,
        SECONDS
    }
}
