package br.com.muxe.core.common.customroom.schema;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.RoomLore.Shown;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoomLoreTest {

    private final RoomLore lore = SchemaFixtures.lore();

    @Test
    void aStepperShowsItsValueAndTheTwoButtons() {
        Shown shown = lore.entry(SchemaFixtures.TEAM_SIZE, RoomOptions.EMPTY);

        assertThat(shown.name()).isEqualTo("&aTamanho do time");
        assertThat(shown.lore()).containsExactly("&7A quantidade de jogadores", "&7por time.", " ", "&7Valor: &a1", " ",
            "&eBotão esquerdo: &fAumentar", "&eBotão direito: &fDiminuir");
    }

    @Test
    void theTeamCountUsesTheValueAsTheAmount() {
        assertThat(lore.entry(SchemaFixtures.TEAM_COUNT, RoomOptions.EMPTY).icon().amount()).isEqualTo(8);
    }

    @Test
    void anOnToggleIsGreenAndGlowsAndAnOffOneIsRed() {
        Shown on = lore.entry(SchemaFixtures.DIAMOND, RoomOptions.EMPTY);
        Shown off = lore.entry(SchemaFixtures.RESTORE, RoomOptions.EMPTY);

        assertThat(on.name()).isEqualTo("&aGerador de Diamante");
        assertThat(on.icon().glow()).isTrue();
        assertThat(on.lore()).endsWith("&7Valor: &aAtivado", " ", "&eClique para desativar.");
        assertThat(off.name()).isEqualTo("&cRestaurar impulso");
        assertThat(off.icon().glow()).isFalse();
        assertThat(off.lore()).endsWith("&7Valor: &cDesativado", " ", "&eClique para ativar.");
    }

    @Test
    void aDecimalAndAChoiceFollowTheKnockbackMenu() {
        Shown vertical = lore.entry(SchemaFixtures.VERTICAL, RoomOptions.EMPTY);
        Shown limiter = lore.entry(SchemaFixtures.LIMITER, RoomOptions.EMPTY);

        assertThat(vertical.lore()).containsExactly("&7Valor base do impulso", "&7vertical.", " ", "&7Valor: &a0.6", " ", "&eClique para alterar.");
        assertThat(limiter.lore()).containsExactly("&7Controla a força do", "&7limitador de cliques", "&7de knockback.", " ",
            "&7- Desativado", "&7- Baixo", "&a- Normal", "&7- Alto", " ", "&eClique para trocar.");
    }

    @Test
    void anEventShowsItsTimeStatusAndButtons() {
        assertThat(lore.entry(SchemaFixtures.DIAMOND_TWO, RoomOptions.EMPTY).lore()).containsExactly(" ", "&7Tempo: &f6:00", "&7Status: &aAtivado", " ",
            "&eBotão esquerdo: &f+30s", "&eBotão direito: &f-30s", "&eShift: &f±1min", "&eQ: &fAtivar/desativar");
        assertThat(lore.entry(SchemaFixtures.GAME_END, RoomOptions.EMPTY).lore()).doesNotContain("&eQ: &fAtivar/desativar");
    }

    @Test
    void aRenamedTeamShowsTheOriginalNameAndTheRestoreButton() {
        Shown plain = lore.entry(SchemaFixtures.FIRST_TEAM, RoomOptions.EMPTY);
        Shown renamed = lore.entry(SchemaFixtures.FIRST_TEAM, new RoomOptions(Map.of("teams.1.name", "Tesão")));
        Shown disabled = lore.entry(SchemaFixtures.FIRST_TEAM, new RoomOptions(Map.of("teams.1.enabled", "false")));

        assertThat(plain.name()).isEqualTo("&fTime #1");
        assertThat(plain.lore()).containsExactly(" ", "&7Status: &aAtivado", " ", "&eBotão esquerdo: &fAtivar/desativar", "&eBotão direito: &fRenomear");
        assertThat(renamed.name()).isEqualTo("&fTesão");
        assertThat(renamed.icon().glow()).isTrue();
        assertThat(renamed.lore()).containsExactly(" ", "&7Nome original: &fTime #1", "&7Status: &aAtivado", " ", "&eBotão esquerdo: &fAtivar/desativar",
            "&eBotão direito: &fRenomear", "&eQ: &fRestaurar nome");
        assertThat(disabled.icon().material()).isEqualTo("BARRIER");
        assertThat(disabled.name()).isEqualTo("&cTime #1");
    }

    @Test
    void aShopItemListsItsStatusPriceAndLimit() {
        Shown wool = lore.entry(SchemaFixtures.WOOL, RoomOptions.EMPTY);
        Shown sword = lore.entry(SchemaFixtures.STONE_SWORD, RoomOptions.EMPTY);

        assertThat(wool.icon().amount()).isEqualTo(16);
        assertThat(wool.lore()).containsExactly(" ", "&7Ótimo para fazer pontes.", " ", "&7Status: &aAtivado", "&7Preço: &f4 Ferros", "&7Limite: &fNenhum", " ",
            "&eClique para alterar.");
        assertThat(sword.lore()).containsExactly(" ", "&7Status: &aAtivado", "&7Preço: &f10 Ferros", "&7Limite: &fNenhum", " ", "&eClique para alterar.");
    }

    @Test
    void changedShopValuesAreMarkedAndGlow() {
        RoomOptions options = new RoomOptions(Map.of("shop.la.enabled", "false", "shop.la.currency", "emerald", "shop.la.limit-type", "player"));

        Map<Integer, Shown> editor = lore.shopEditor(SchemaFixtures.WOOL, options);

        assertThat(editor.get(RoomLore.SHOP_STATUS_SLOT).lore()).containsExactly(" ", "&7Valor: &cDesativado*", " ", "&eClique para ativar.");
        assertThat(editor.get(RoomLore.SHOP_STATUS_SLOT).icon()).isEqualTo(RoomLore.STATUS_OFF_ICON.withGlow(true));
        assertThat(editor.get(RoomLore.SHOP_PRICE_SLOT).lore()).containsExactly(" ", "&7Valor: &24 Esmeraldas*", " ", "&eBotão esquerdo: &fAlterar valor",
            "&eBotão direito: &fAlterar moeda");
        assertThat(editor.get(RoomLore.SHOP_PRICE_SLOT).icon().amount()).isEqualTo(4);
        assertThat(editor.get(RoomLore.SHOP_LIMIT_SLOT).lore()).containsExactly(" ", "&7Valor: &f1 por jogador*", " ", "&eBotão esquerdo: &fAlterar quantidade",
            "&eBotão direito: &fAlterar tipo");
        assertThat(lore.entry(SchemaFixtures.WOOL, options).icon().glow()).isTrue();
    }

    @Test
    void anUnchangedLimitOnlyOffersTheType() {
        assertThat(lore.shopEditor(SchemaFixtures.WOOL, RoomOptions.EMPTY).get(RoomLore.SHOP_LIMIT_SLOT).lore())
            .containsExactly(" ", "&7Valor: &fNenhum", " ", "&eBotão direito: &fAlterar tipo");
    }

    @Test
    void anUpgradeListsEveryLevelAndItsEditorShowsEachOne() {
        Shown armor = lore.entry(SchemaFixtures.ARMOR, RoomOptions.EMPTY);
        Map<Integer, Shown> editor = lore.upgradeEditor(SchemaFixtures.ARMOR, RoomOptions.EMPTY);

        assertThat(armor.lore()).containsExactly(" ", "&7O seu time ganha Proteção em todas", "&7as peças armaduras.", " ", "&7Status: &aAtivado",
            "&7Nível I: &b2 Diamantes", "&7Nível II: &b4 Diamantes", "&7Nível III: &b8 Diamantes", "&7Nível IV: &b16 Diamantes", " ", "&eClique para alterar.");
        assertThat(editor).containsOnlyKeys(10, 12, 13, 14, 15);
        assertThat(editor.get(12).name()).isEqualTo("&aNível I");
        assertThat(editor.get(12).lore()).containsExactly(" ", "&7Proteção I", " ", "&7Preço: &b2 Diamantes", " ", "&eBotão esquerdo: &fAlterar valor",
            "&eBotão direito: &fAlterar moeda");
        assertThat(editor.get(15).icon().amount()).isEqualTo(16);
    }

    @Test
    void aDisabledShopItemShowsABarrierAndAnEnabledOneItsOwnIcon() {
        RoomOptions enabled = new RoomOptions(Map.of("shop.troca-congelante.enabled", "true"));

        assertThat(lore.entry(SchemaFixtures.WOOL, new RoomOptions(Map.of("shop.la.enabled", "false"))).icon().material()).isEqualTo("BARRIER");
        assertThat(lore.entry(SchemaFixtures.WOOL, RoomOptions.EMPTY).icon().material()).isEqualTo("WOOL");
        assertThat(lore.entry(SchemaFixtures.ICE_BRIDGE, RoomOptions.EMPTY).icon().material()).isEqualTo("BARRIER");
        assertThat(lore.entry(SchemaFixtures.ICE_BRIDGE, new RoomOptions(Map.of("shop.ponte-de-gelo.enabled", "true"))).icon().material()).isEqualTo("ICE");
        assertThat(lore.entry(SchemaFixtures.FROZEN_SWAP, enabled).lore()).contains("&7Status: &aAtivado*");
    }

    @Test
    void aDisabledUpgradeShowsABarrierAndAnEnabledOneItsOwnIcon() {
        RoomOptions disabled = new RoomOptions(Map.of("upgrades.reforco.enabled", "false"));

        assertThat(lore.entry(SchemaFixtures.ARMOR, disabled).icon().material()).isEqualTo("BARRIER");
        assertThat(lore.entry(SchemaFixtures.ARMOR, disabled).lore()).contains("&7Status: &cDesativado");
        assertThat(lore.entry(SchemaFixtures.ARMOR, RoomOptions.EMPTY).icon().material()).isEqualTo("IRON_CHESTPLATE");
    }

    @Test
    void everythingFreeMarksThePricesAndTurnsTheButtonGreen() {
        RoomOptions free = new RoomOptions(Map.of("upgrades.free", "true", "shop.free", "true"));

        assertThat(lore.entry(SchemaFixtures.ARMOR, free).lore()).contains("&7Nível I: &aGrátis*");
        assertThat(lore.entry(SchemaFixtures.ARMOR, free).icon().glow()).isTrue();
        assertThat(lore.entry(SchemaFixtures.WOOL, free).lore()).contains("&7Preço: &aGrátis*");
        Shown button = lore.entry(SchemaFixtures.SHOP_FREE, free);
        assertThat(button.name()).isEqualTo("&aTudo grátis");
        assertThat(button.lore()).endsWith("&eClique para voltar ao preço padrão.");
        assertThat(lore.entry(SchemaFixtures.SHOP_FREE, RoomOptions.EMPTY).name()).isEqualTo("&cTudo grátis");
    }
}
