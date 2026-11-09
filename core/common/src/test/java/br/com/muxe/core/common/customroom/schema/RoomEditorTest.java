package br.com.muxe.core.common.customroom.schema;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomEditor.Click;
import br.com.muxe.core.common.customroom.schema.RoomEditor.LimitType;
import br.com.muxe.core.common.customroom.schema.RoomEditor.Outcome;
import br.com.muxe.core.common.customroom.schema.RoomEditor.PromptKind;
import br.com.muxe.core.common.customroom.schema.RoomEditor.ShopField;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomEditorTest {

    private final RoomEditor editor = SchemaFixtures.editor();

    @Test
    void theTeamSizeStepsWithinTheModeAndDropsTheOverrideAtTheDefault() {
        RoomOptions bigger = changed(editor.click(SchemaFixtures.TEAM_SIZE, Click.LEFT, RoomOptions.EMPTY));

        assertThat(editor.stepper(SchemaFixtures.TEAM_SIZE, bigger)).isEqualTo(2);
        RoomOptions back = changed(editor.click(SchemaFixtures.TEAM_SIZE, Click.RIGHT, bigger));
        assertThat(back.values()).isEmpty();
        assertThat(editor.click(SchemaFixtures.TEAM_SIZE, Click.RIGHT, back)).isEqualTo(Outcome.NOTHING);
    }

    @Test
    void theTeamSizeStopsAtThePlayersOfTheMode() {
        RoomOptions options = new RoomOptions(Map.of("team-size", "8"));

        assertThat(editor.click(SchemaFixtures.TEAM_SIZE, Click.LEFT, options)).isEqualTo(Outcome.NOTHING);
    }

    @Test
    void multipliersMoveByQuartersAndShowAComma() {
        RoomOptions faster = changed(editor.click(SchemaFixtures.DIAMOND_SPEED, Click.LEFT, RoomOptions.EMPTY));

        assertThat(RoomEditor.format(SchemaFixtures.DIAMOND_SPEED, editor.stepper(SchemaFixtures.DIAMOND_SPEED, faster))).isEqualTo("1,25x");
        assertThat(faster.get("generators.diamond.speed")).contains("1.25");
    }

    @Test
    void togglesFlipAndChoicesCycle() {
        RoomOptions off = changed(editor.click(SchemaFixtures.DIAMOND, Click.LEFT, RoomOptions.EMPTY));
        RoomOptions high = changed(editor.click(SchemaFixtures.LIMITER, Click.LEFT, RoomOptions.EMPTY));
        RoomOptions offLimiter = changed(editor.click(SchemaFixtures.LIMITER, Click.LEFT, high));

        assertThat(editor.toggle(SchemaFixtures.DIAMOND, off)).isFalse();
        assertThat(editor.choice(SchemaFixtures.LIMITER, high).id()).isEqualTo("high");
        assertThat(editor.choice(SchemaFixtures.LIMITER, offLimiter).id()).isEqualTo("off");
    }

    @Test
    void decimalsAreAskedAndValidated() {
        Outcome outcome = editor.click(SchemaFixtures.VERTICAL, Click.LEFT, RoomOptions.EMPTY);

        RoomEditor.Prompt prompt = ((Outcome.Ask) outcome).prompt();
        assertThat(prompt.kind()).isEqualTo(PromptKind.DECIMAL);
        assertThat(prompt.current()).isEqualTo("0.6");
        assertThat(editor.answer(prompt, "0,8", RoomOptions.EMPTY).orElseThrow().get("knockback.vertical")).contains("0.8");
        assertThat(editor.answer(prompt, "0.60", RoomOptions.EMPTY).orElseThrow().values()).isEmpty();
        assertThat(editor.answer(prompt, "abc", RoomOptions.EMPTY)).isEmpty();
        assertThat(editor.answer(prompt, "9", RoomOptions.EMPTY)).isEmpty();
    }

    @Test
    void timersMoveByHalfMinutesOrMinutesWithShiftAndToggleWithQ() {
        RoomOptions later = changed(editor.click(SchemaFixtures.DIAMOND_TWO, Click.LEFT, RoomOptions.EMPTY));
        RoomOptions muchEarlier = changed(editor.click(SchemaFixtures.DIAMOND_TWO, Click.SHIFT_RIGHT, RoomOptions.EMPTY));
        RoomOptions disabled = changed(editor.click(SchemaFixtures.DIAMOND_TWO, Click.DROP, RoomOptions.EMPTY));

        assertThat(editor.timerSeconds(SchemaFixtures.DIAMOND_TWO, later)).isEqualTo(390);
        assertThat(editor.timerSeconds(SchemaFixtures.DIAMOND_TWO, muchEarlier)).isEqualTo(300);
        assertThat(editor.timerEnabled(SchemaFixtures.DIAMOND_TWO, disabled)).isFalse();
        assertThat(editor.click(SchemaFixtures.GAME_END, Click.DROP, RoomOptions.EMPTY)).isEqualTo(Outcome.NOTHING);
    }

    @Test
    void teamsToggleRenameAndRestore() {
        RoomOptions disabled = changed(editor.click(SchemaFixtures.FIRST_TEAM, Click.LEFT, RoomOptions.EMPTY));
        Outcome rename = editor.click(SchemaFixtures.FIRST_TEAM, Click.RIGHT, RoomOptions.EMPTY);
        RoomOptions renamed = editor.answer(((Outcome.Ask) rename).prompt(), " Tesão ", RoomOptions.EMPTY).orElseThrow();
        RoomOptions restored = changed(editor.click(SchemaFixtures.FIRST_TEAM, Click.DROP, renamed));

        assertThat(editor.teamEnabled(1, disabled)).isFalse();
        assertThat(editor.teamName(1, renamed)).contains("Tesão");
        assertThat(restored.values()).isEmpty();
        assertThat(editor.answer(((Outcome.Ask) rename).prompt(), "x".repeat(17), RoomOptions.EMPTY)).isEmpty();
    }

    @Test
    void theTeamCountDisablesTheTeamsAboveIt() {
        RoomOptions six = new RoomOptions(Map.of("teams.count", "6"));

        assertThat(editor.teamCount(six)).isEqualTo(6);
        assertThat(editor.teamEnabled(6, six)).isTrue();
        assertThat(editor.teamEnabled(7, six)).isFalse();
        assertThat(editor.teamCount(RoomOptions.EMPTY)).isEqualTo(8);
    }

    @Test
    void shopItemsChangeStatusPriceCurrencyAndLimit() {
        RoomOptions off = changed(editor.shopClick(SchemaFixtures.WOOL, ShopField.STATUS, Click.LEFT, RoomOptions.EMPTY));
        RoomOptions gold = changed(editor.shopClick(SchemaFixtures.WOOL, ShopField.PRICE, Click.RIGHT, RoomOptions.EMPTY));
        RoomOptions team = changed(editor.shopClick(SchemaFixtures.WOOL, ShopField.LIMIT, Click.RIGHT, RoomOptions.EMPTY));
        Outcome askPrice = editor.shopClick(SchemaFixtures.WOOL, ShopField.PRICE, Click.LEFT, RoomOptions.EMPTY);
        RoomOptions eight = editor.answer(((Outcome.Ask) askPrice).prompt(), "8", RoomOptions.EMPTY).orElseThrow();

        assertThat(editor.shop(SchemaFixtures.WOOL, off).enabled()).isFalse();
        assertThat(editor.shop(SchemaFixtures.WOOL, gold).currency()).isEqualTo("gold");
        assertThat(editor.shop(SchemaFixtures.WOOL, team).limitType()).isEqualTo(LimitType.TEAM);
        assertThat(editor.shop(SchemaFixtures.WOOL, eight).price()).isEqualTo(8);
        assertThat(editor.shopClick(SchemaFixtures.WOOL, ShopField.LIMIT, Click.LEFT, RoomOptions.EMPTY)).isEqualTo(Outcome.NOTHING);
        assertThat(editor.shopClick(SchemaFixtures.WOOL, ShopField.LIMIT, Click.LEFT, team)).isInstanceOf(Outcome.Ask.class);
    }

    @Test
    void upgradeLevelsChangePriceAndCurrency() {
        RoomOptions emerald = changed(editor.upgradeClick(SchemaFixtures.ARMOR, 2, Click.RIGHT, RoomOptions.EMPTY));
        Outcome ask = editor.upgradeClick(SchemaFixtures.ARMOR, 1, Click.LEFT, RoomOptions.EMPTY);
        RoomOptions cheap = editor.answer(((Outcome.Ask) ask).prompt(), "1", RoomOptions.EMPTY).orElseThrow();
        RoomOptions disabled = changed(editor.upgradeClick(SchemaFixtures.ARMOR, 0, Click.LEFT, RoomOptions.EMPTY));

        assertThat(editor.levelCurrency(SchemaFixtures.ARMOR, 2, emerald)).isEqualTo("emerald");
        assertThat(editor.levelPrice(SchemaFixtures.ARMOR, 1, cheap)).isEqualTo(1);
        assertThat(editor.upgradeEnabled(SchemaFixtures.ARMOR, disabled)).isFalse();
        assertThat(editor.answer(((Outcome.Ask) ask).prompt(), "2", RoomOptions.EMPTY).orElseThrow().values()).isEmpty();
    }

    @Test
    void linksOpenTheirPageAndListsOpenTheirEditors() {
        assertThat(editor.click(SchemaFixtures.TEAMS_LINK, Click.LEFT, RoomOptions.EMPTY)).isEqualTo(new Outcome.Open("teams"));
        assertThat(editor.click(SchemaFixtures.WOOL, Click.LEFT, RoomOptions.EMPTY)).isEqualTo(new Outcome.EditShop(SchemaFixtures.WOOL));
        assertThat(editor.click(SchemaFixtures.ARMOR, Click.LEFT, RoomOptions.EMPTY)).isEqualTo(new Outcome.EditUpgrade(SchemaFixtures.ARMOR));
    }

    @Test
    void everythingFreeIsAToggleOfItsScope() {
        RoomOptions free = changed(editor.click(SchemaFixtures.SHOP_FREE, Click.LEFT, RoomOptions.EMPTY));

        assertThat(editor.free("shop", free)).isTrue();
        assertThat(editor.shop(SchemaFixtures.WOOL, free).free()).isTrue();
        assertThat(changed(editor.click(SchemaFixtures.SHOP_FREE, Click.LEFT, free)).values()).isEmpty();
    }

    private static RoomOptions changed(Outcome outcome) {
        assertThat(outcome).isInstanceOf(Outcome.Changed.class);
        return ((Outcome.Changed) outcome).options();
    }
}
