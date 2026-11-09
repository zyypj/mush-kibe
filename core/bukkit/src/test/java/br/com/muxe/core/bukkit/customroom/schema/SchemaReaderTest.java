package br.com.muxe.core.bukkit.customroom.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Format;
import br.com.muxe.core.common.customroom.schema.OptionPage;
import br.com.muxe.core.common.customroom.schema.RoomSchema;
import org.junit.jupiter.api.Test;

class SchemaReaderTest {

    private static final String YAML = """
        root: options
        currencies:
          iron:
            one: Ferro
            many: Ferros
            color: "&f"
            material: IRON_INGOT
          diamond:
            one: Diamante
            many: Diamantes
            color: "&b"
            material: DIAMOND
        pages:
          options:
            title: "Opções"
            rows: 4
            back-slot: 30
            entries:
              team-size:
                type: stepper
                slot: 10
                material: SKULL_ITEM
                data: 3
                name: "Tamanho do time"
                description:
                  - "&7A quantidade de jogadores"
                default: mode-team-size
                min: "1"
                max: mode-players
                step: 1
              knockback:
                type: link
                slot: 19
                material: STICK
                name: Knockback
                page: knockback
                lore:
                  - "&eClique para ver mais."
          knockback:
            title: "Alterar knockback"
            rows: 4
            back-slot: 30
            back-to: options
            entries:
              vertical:
                type: decimal
                slot: 10
                key: knockback.vertical
                material: FEATHER
                name: Vertical
                default: "0.6"
              limiter:
                type: choice
                slot: 25
                key: knockback.click-limiter
                material: STONE_BUTTON
                name: "Limitador de cliques"
                default: normal
                values:
                  normal:
                    label: Normal
                  high:
                    label: Alto
              forge:
                type: stepper
                slot: 11
                key: generators.forge.speed
                material: FURNACE
                name: "Velocidade da forja"
                default: "1"
                min: "0.25"
                max: "5"
                step: 0.25
                format: multiplier
          upgrades:
            title: "Melhorias (1/1)"
            rows: 5
            entries:
              reforco:
                type: upgrade
                slot: 11
                material: IRON_CHESTPLATE
                name: "Reforço na Armadura"
                levels:
                  '1':
                    label: "Proteção I"
                    price: 2
                    currency: diamond
                  '2':
                    label: "Proteção II"
                    price: 4
                    currency: diamond
        """;

    @Test
    void readsPagesEntriesAndCurrencies() {
        RoomSchema schema = SchemaReader.parse("bedwars", YAML);

        assertThat(schema.root()).isEqualTo("options");
        assertThat(schema.currencies()).containsOnlyKeys("iron", "diamond");
        assertThat(schema.nextCurrency("diamond")).isEqualTo("iron");
        OptionPage options = schema.page("options").orElseThrow();
        assertThat(options.rows()).isEqualTo(4);
        OptionEntry.Stepper teamSize = (OptionEntry.Stepper) options.at(10).orElseThrow();
        assertThat(teamSize.key()).isEqualTo("team-size");
        assertThat(teamSize.icon().data()).isEqualTo(3);
        assertThat(teamSize.max()).isEqualTo("mode-players");
        assertThat(options.at(19).orElseThrow()).isInstanceOf(OptionEntry.Link.class);
    }

    @Test
    void readsDecimalsChoicesMultipliersAndUpgradeLevels() {
        RoomSchema schema = SchemaReader.parse("bedwars", YAML);

        OptionPage knockback = schema.page("knockback").orElseThrow();
        assertThat(knockback.backTo()).isEqualTo("options");
        assertThat(((OptionEntry.Decimal) knockback.at(10).orElseThrow()).defaultValue()).isEqualTo("0.6");
        OptionEntry.Choice limiter = (OptionEntry.Choice) knockback.at(25).orElseThrow();
        assertThat(limiter.values()).extracting(OptionEntry.ChoiceValue::label).containsExactly("Normal", "Alto");
        assertThat(limiter.values().getFirst().icon().material()).isEqualTo("STONE_BUTTON");
        assertThat(((OptionEntry.Stepper) knockback.at(11).orElseThrow()).format()).isEqualTo(Format.MULTIPLIER);
        OptionEntry.Upgrade armor = (OptionEntry.Upgrade) schema.page("upgrades").orElseThrow().at(11).orElseThrow();
        assertThat(armor.levels()).extracting(OptionEntry.Level::label).containsExactly("Proteção I", "Proteção II");
        assertThat(armor.enabled()).isTrue();
    }

    @Test
    void refusesAnUnknownEntryType() {
        assertThatThrownBy(() -> SchemaReader.parse("x", """
            pages:
              options:
                entries:
                  odd:
                    type: wat
            """)).isInstanceOf(IllegalArgumentException.class);
    }
}
