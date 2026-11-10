package br.com.muxe.minigame.bedwars.hotbar.menu;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.language.LocalizedConfig;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuButton;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.minigame.bedwars.BedWarsPlugin;
import br.com.muxe.minigame.bedwars.hotbar.HotbarChoices;
import br.com.muxe.minigame.bedwars.hotbar.domain.HotbarEntry;
import br.com.muxe.minigame.bedwars.hotbar.domain.HotbarLayout;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarEditor.AddingTab;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarEditor.Back;
import br.com.muxe.minigame.bedwars.hotbar.menu.HotbarFixtures.MemoryStore;
import br.com.muxe.minigame.bedwars.room.RoomFixtures;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog;
import br.com.muxe.minigame.bedwars.shop.ShopCatalog.ShopItem;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

class HotbarEditorTest {

    private static final HotbarEntry BLOCKS = HotbarEntry.category("blocos");
    private static final HotbarEntry WOOL = HotbarEntry.item("la");
    private static final HotbarEntry PICKAXE = HotbarEntry.item("picareta");

    private final ShopCatalog catalog = ShopCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("shop"));
    private final HotbarMenuSettings settings = HotbarMenuSettings.from(RoomFixtures.CONFIG.getConfigurationSection("shop.hotbar-menus"));
    private final HotbarChoices choices = new HotbarChoices(catalog);
    private final MemoryStore store = new MemoryStore();
    private final MessageCatalog messages = MessageCatalog.from(RoomFixtures.CONFIG.getConfigurationSection("messages"));
    private final List<Menu> opened = new ArrayList<>();
    private final List<String> sent = new ArrayList<>();
    private final List<Player> returned = new ArrayList<>();
    private final Player player = HotbarFixtures.player(sent);
    private final HotbarEditor editor = new HotbarEditor(settings, choices, new HotbarIcons(catalog, choices, settings.adding()), store, messages,
        (viewer, menu) -> opened.add(menu));
    private final Back back = new Back("Menu do Bed Wars", returned::add);

    private HotbarLayout fullSlotZero() {
        HotbarLayout full = HotbarLayout.empty();
        for (ShopItem item : choices.items().subList(0, HotbarLayout.MAX_ENTRIES)) {
            full = full.add(0, HotbarEntry.item(item.id()));
        }
        return full;
    }

    private Menu editing() {
        return editor.editing(HotbarFixtures.PLAYER, back);
    }

    private static ItemBuilder icon(Menu menu, int slot) {
        return menu.button(slot).orElseThrow(() -> new AssertionError("No button at slot " + slot)).icon();
    }

    private Menu lastOpened() {
        return opened.getLast();
    }

    @Test
    void theSettingsFollowTheReference() {
        assertThat(settings.frameTicks()).isEqualTo(20);
        assertThat(settings.editing().title()).isEqualTo("Editando hotbar...");
        assertThat(settings.editing().rows()).isEqualTo(3);
        assertThat(settings.adding().title()).isEqualTo("Adicionado no slot #{slot}...");
        assertThat(settings.adding().rows()).isEqualTo(6);
        assertThat(settings.removing().title()).isEqualTo("Removendo do slot #{slot}...");
        assertThat(settings.removing().rows()).isEqualTo(4);
        assertThat(HotbarEditor.REMOVING_SLOTS).hasSize(HotbarLayout.MAX_ENTRIES);
        assertThat(HotbarEditor.ADDING_SLOTS).hasSize(21).startsWith(19, 20).endsWith(42, 43);
    }

    @Test
    void anEnglishReaderEditsTheHotbarInEnglish() {
        LocalizedConfig texts = LocalizedConfig.bundled(BedWarsPlugin.class, "config.yml");
        Localized<ShopCatalog> shops = texts.map(root -> ShopCatalog.from(root.getConfigurationSection("shop")));
        Localized<HotbarMenuSettings> menus = texts.map(root -> HotbarMenuSettings.from(root.getConfigurationSection("shop.hotbar-menus")));
        Localized<HotbarChoices> localizedChoices = shops.map(HotbarChoices::new);
        HotbarEditor english = new HotbarEditor(menus, localizedChoices,
            Localized.of(language -> new HotbarIcons(shops.in(language), localizedChoices.in(language), menus.in(language).adding())), id -> Language.ENGLISH, store,
            MessageCatalog.from(texts, "messages"), (viewer, menu) -> opened.add(menu));

        english.openFromBedWarsMenu(player, returned::add);

        assertThat(lastOpened().title()).isEqualTo("Editing hotbar...");
        assertThat(english.adding(0, AddingTab.CATEGORIES, 1, new Back("", returned::add, Language.ENGLISH)).title()).isEqualTo(
            menus.in(Language.ENGLISH).adding().title().replace("{slot}", "1"));
        assertThat(editing().title()).isEqualTo("Editando hotbar...");
    }

    @Test
    void anEmptyHotbarShowsNineRedPanesThatInviteToAdd() {
        Menu menu = editing();

        assertThat(menu.title()).isEqualTo("Editando hotbar...");
        assertThat(menu.rows()).isEqualTo(3);
        for (int slot = 0; slot < HotbarLayout.SLOTS; slot++) {
            ItemBuilder pane = icon(menu, 9 + slot);
            assertThat(pane.material()).isEqualTo(Material.STAINED_GLASS_PANE);
            assertThat(pane.data()).isEqualTo((short) 14);
            assertThat(pane.name()).isEqualTo("§aSlot #" + (slot + 1));
            assertThat(pane.lore()).containsExactly(" ", "§eBotão esquerdo: §fAdicionar");
            assertThat(pane.hidesFlags()).isFalse();
            assertThat(menu.button(9 + slot).orElseThrow().animated()).isFalse();
        }
    }

    @Test
    void theBackArrowNamesWhereItGoes() {
        ItemBuilder arrow = icon(editing(), 21);

        assertThat(arrow.material()).isEqualTo(Material.ARROW);
        assertThat(arrow.name()).isEqualTo("§aVoltar");
        assertThat(arrow.lore()).containsExactly("§7Para Menu do Bed Wars");
        assertThat(icon(editor.editing(HotbarFixtures.PLAYER, new Back(settings.shop(), returned::add)), 21).lore()).containsExactly("§7Para Compra Rápida");
    }

    @Test
    void clickingBackLeavesTheEditor() {
        HotbarFixtures.click(editing(), 21, ClickType.LEFT, player);

        assertThat(returned).containsExactly(player);
    }

    @Test
    void aSlotWithOneEntryShowsItsIconAndSaysSelecionado() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(2, BLOCKS));

        MenuButton button = editing().button(11).orElseThrow();

        assertThat(button.animated()).isFalse();
        assertThat(button.icon().material()).isEqualTo(Material.HARD_CLAY);
        assertThat(button.icon().name()).isEqualTo("§aSlot #3");
        assertThat(button.icon().hidesFlags()).isTrue();
        assertThat(button.icon().lore()).containsExactly(
            " ", "§eSelecionado:", "§a- Blocos", " ", "§eBotão esquerdo: §fAdicionar", "§eBotão direito: §fRemover");
    }

    @Test
    void aSlotWithSeveralEntriesCyclesIconAndHighlightEverySecond() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(0, BLOCKS).add(0, WOOL).add(0, PICKAXE));

        MenuButton button = editing().button(9).orElseThrow();

        assertThat(button.animated()).isTrue();
        assertThat(button.period()).isEqualTo(20);
        assertThat(button.frames()).hasSize(3);

        ItemBuilder first = button.iconAt(0);
        assertThat(first.material()).isEqualTo(Material.HARD_CLAY);
        assertThat(first.name()).isEqualTo("§aSlot #1");
        assertThat(first.lore()).containsExactly(
            " ", "§eSelecionados:", "§a- Blocos", "§7- Lã", "§7- Picareta", " ", "§eBotão esquerdo: §fAdicionar", "§eBotão direito: §fRemover");

        ItemBuilder second = button.iconAt(20);
        assertThat(second.material()).isEqualTo(Material.WOOL);
        assertThat(second.amount()).isEqualTo(16);
        assertThat(second.name()).isEqualTo("§aSlot #1");
        assertThat(second.lore()).containsExactly(
            " ", "§eSelecionados:", "§7- Blocos", "§a- Lã", "§7- Picareta", " ", "§eBotão esquerdo: §fAdicionar", "§eBotão direito: §fRemover");

        ItemBuilder third = button.iconAt(40);
        assertThat(third.material()).isEqualTo(Material.DIAMOND_PICKAXE);
        assertThat(third.lore()).contains("§7- Blocos", "§7- Lã", "§a- Picareta");

        assertThat(button.iconAt(60)).isEqualTo(first);
        assertThat(button.changesAt(20)).isTrue();
        assertThat(button.changesAt(10)).isFalse();
    }

    @Test
    void theLoreOfASlotListsEveryEntryAndMarksTheOneShown() {
        assertThat(HotbarEditor.slotLore(List.of("Lã"), 0, settings.editing())).containsExactly(
            " ", "&eSelecionado:", "&a- Lã", " ", "&eBotão esquerdo: &fAdicionar", "&eBotão direito: &fRemover");
        assertThat(HotbarEditor.slotLore(List.of("Lã", "Argila"), 1, settings.editing())).containsExactly(
            " ", "&eSelecionados:", "&7- Lã", "&a- Argila", " ", "&eBotão esquerdo: &fAdicionar", "&eBotão direito: &fRemover");
    }

    @Test
    void leftClickingASlotOpensTheAddMenuForThatSlot() {
        HotbarFixtures.click(editing(), 13, ClickType.LEFT, player);

        assertThat(lastOpened().title()).isEqualTo("Adicionado no slot #5...");
    }

    @Test
    void rightClickingAnEmptySlotDoesNothing() {
        HotbarFixtures.click(editing(), 13, ClickType.RIGHT, player);

        assertThat(opened).isEmpty();
    }

    @Test
    void rightClickingASlotWithEntriesOpensTheRemoveMenu() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(4, WOOL));

        HotbarFixtures.click(editing(), 13, ClickType.RIGHT, player);

        assertThat(lastOpened().title()).isEqualTo("Removendo do slot #5...");
    }

    @Test
    void leftClickingAFullSlotWarnsInsteadOfOpeningTheAddMenu() {
        HotbarLayout full = fullSlotZero();
        store.change(HotbarFixtures.PLAYER, full);

        HotbarFixtures.click(editing(), 9, ClickType.LEFT, player);

        assertThat(opened).isEmpty();
        assertThat(sent).containsExactly("§cEste slot da hotbar já tem o máximo de entradas.");
    }

    @Test
    void theCategoriesTabListsEveryCategoryButTheArmorOne() {
        Menu menu = editor.adding(2, AddingTab.CATEGORIES, 1, back);

        assertThat(menu.title()).isEqualTo("Adicionado no slot #3...");
        assertThat(menu.rows()).isEqualTo(6);
        ItemBuilder categories = icon(menu, 0);
        assertThat(categories.material()).isEqualTo(Material.BOOK);
        assertThat(categories.name()).isEqualTo("§aCategorias");
        assertThat(categories.lore()).isEmpty();
        ItemBuilder items = icon(menu, 1);
        assertThat(items.material()).isEqualTo(Material.DIAMOND_PICKAXE);
        assertThat(items.name()).isEqualTo("§aItens");
        assertThat(items.lore()).containsExactly("§eClique para ver mais.");

        List<Material> icons = List.of(Material.HARD_CLAY, Material.GOLD_SWORD, Material.STONE_PICKAXE, Material.BOW, Material.BREWING_STAND_ITEM, Material.TNT);
        List<String> names = List.of("Blocos", "Combate", "Ferramentas", "Arcos", "Poções", "Utilidades");
        for (int index = 0; index < icons.size(); index++) {
            ItemBuilder category = icon(menu, HotbarEditor.ADDING_SLOTS.get(index));
            assertThat(category.material()).isEqualTo(icons.get(index));
            assertThat(category.name()).isEqualTo("§a" + names.get(index));
            assertThat(category.hidesFlags()).isTrue();
            assertThat(category.lore()).containsExactly(" ", "§7Incluí todos os itens desta categoria.", " ", "§eClique para adicionar.");
        }
        assertThat(menu.button(HotbarEditor.ADDING_SLOTS.get(icons.size()))).isEmpty();
    }

    @Test
    void aGreenPaneMarksTheOpenTab() {
        Menu categories = editor.adding(0, AddingTab.CATEGORIES, 1, back);
        Menu items = editor.adding(0, AddingTab.ITEMS, 1, back);

        for (int column = 0; column < 9; column++) {
            ItemBuilder onCategories = icon(categories, 9 + column);
            ItemBuilder onItems = icon(items, 9 + column);
            assertThat(onCategories.material()).isEqualTo(Material.STAINED_GLASS_PANE);
            assertThat(onCategories.name()).isEqualTo(" ");
            assertThat(onCategories.data()).isEqualTo((short) (column == 0 ? 13 : 7));
            assertThat(onItems.data()).isEqualTo((short) (column == 1 ? 13 : 7));
        }
        assertThat(icon(items, 0).lore()).containsExactly("§eClique para ver mais.");
        assertThat(icon(items, 1).lore()).isEmpty();
    }

    @Test
    void theTabsSwitchTheListWithoutLeavingTheSlot() {
        HotbarFixtures.click(editor.adding(6, AddingTab.CATEGORIES, 1, back), 1, ClickType.LEFT, player);
        assertThat(lastOpened().title()).isEqualTo("Adicionado no slot #7...");
        assertThat(icon(lastOpened(), 19).name()).isEqualTo("§aLã");

        HotbarFixtures.click(lastOpened(), 0, ClickType.LEFT, player);
        assertThat(icon(lastOpened(), 19).name()).isEqualTo("§aBlocos");
    }

    @Test
    void theItemsTabShowsEachItemWithItsShopLook() {
        Menu menu = editor.adding(0, AddingTab.ITEMS, 1, back);

        ItemBuilder wool = icon(menu, 19);
        assertThat(wool.material()).isEqualTo(Material.WOOL);
        assertThat(wool.amount()).isEqualTo(16);
        assertThat(wool.name()).isEqualTo("§aLã");
        assertThat(wool.hidesFlags()).isTrue();
        assertThat(wool.lore()).containsExactly(" ", "§7Ótimo para fazer pontes.", " ", "§eClique para adicionar.");
        assertThat(icon(menu, 20).name()).isEqualTo("§aArgila");
        assertThat(icon(menu, 20).lore()).containsExactly(" ", "§7Bloco básico para proteger a sua", "§7cama.", " ", "§eClique para adicionar.");
        assertThat(icon(menu, 43).name()).isNotNull();
    }

    @Test
    void anItemWithoutDescriptionOnlyShowsTheClickLine() {
        Menu menu = editor.adding(0, AddingTab.ITEMS, 2, back);

        assertThat(icon(menu, 19).name()).isEqualTo("§aMaçã Dourada");
        assertThat(icon(menu, 19).lore()).containsExactly(" ", "§eClique para adicionar.");
    }

    @Test
    void theItemsTabPaginatesTwentyOneToAPage() {
        Menu first = editor.adding(0, AddingTab.ITEMS, 1, back);
        Menu second = editor.adding(0, AddingTab.ITEMS, 2, back);

        assertThat(HotbarEditor.ADDING_SLOTS.stream().filter(slot -> first.button(slot).isPresent())).hasSize(21);
        assertThat(HotbarEditor.ADDING_SLOTS.stream().filter(slot -> second.button(slot).isPresent())).hasSize(choices.items().size() - 21);

        assertThat(icon(first, 50).name()).isEqualTo("§aPágina 2");
        assertThat(icon(first, 50).material()).isEqualTo(Material.ARROW);
        assertThat(icon(first, 48).name()).isEqualTo("§aVoltar");
        assertThat(icon(first, 48).lore()).containsExactly("§7Para Editando hotbar...");

        assertThat(icon(second, 48).name()).isEqualTo("§aPágina 1");
        assertThat(icon(second, 48).lore()).isEmpty();
        assertThat(second.button(50)).isEmpty();
    }

    @Test
    void theArrowsMoveBetweenPagesAndVoltarGoesBackToTheEditor() {
        HotbarFixtures.click(editor.adding(3, AddingTab.ITEMS, 1, back), 50, ClickType.LEFT, player);
        assertThat(icon(lastOpened(), 48).name()).isEqualTo("§aPágina 1");

        HotbarFixtures.click(lastOpened(), 48, ClickType.LEFT, player);
        assertThat(icon(lastOpened(), 50).name()).isEqualTo("§aPágina 2");

        HotbarFixtures.click(lastOpened(), 48, ClickType.LEFT, player);
        assertThat(lastOpened().title()).isEqualTo("Editando hotbar...");
    }

    @Test
    void choosingACategoryAddsItToTheEndOfTheSlotSavesAndReturnsToTheEditor() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(1, WOOL));

        HotbarFixtures.click(editor.adding(1, AddingTab.CATEGORIES, 1, back), 19, ClickType.LEFT, player);

        assertThat(store.of(HotbarFixtures.PLAYER).entries(1)).containsExactly(WOOL, BLOCKS);
        assertThat(store.saves()).hasSize(2);
        assertThat(lastOpened().title()).isEqualTo("Editando hotbar...");
    }

    @Test
    void choosingAnItemAlreadyInAnotherSlotMovesIt() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(0, WOOL).add(0, PICKAXE));

        HotbarFixtures.click(editor.adding(5, AddingTab.ITEMS, 1, back), 19, ClickType.LEFT, player);

        assertThat(store.of(HotbarFixtures.PLAYER).entries(0)).containsExactly(PICKAXE);
        assertThat(store.of(HotbarFixtures.PLAYER).entries(5)).containsExactly(WOOL);
    }

    @Test
    void choosingWhatIsAlreadyInTheSlotSavesNothingAndStillReturns() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(2, WOOL));

        HotbarFixtures.click(editor.adding(2, AddingTab.ITEMS, 1, back), 19, ClickType.LEFT, player);

        assertThat(store.saves()).hasSize(1);
        assertThat(store.of(HotbarFixtures.PLAYER).entries(2)).containsExactly(WOOL);
        assertThat(lastOpened().title()).isEqualTo("Editando hotbar...");
    }

    @Test
    void aFullSlotRefusesANewEntryAndChangesNothing() {
        HotbarLayout full = fullSlotZero();
        store.change(HotbarFixtures.PLAYER, full);

        HotbarFixtures.click(editor.adding(0, AddingTab.ITEMS, 1, back), HotbarEditor.ADDING_SLOTS.get(HotbarLayout.MAX_ENTRIES + 1), ClickType.LEFT, player);

        assertThat(store.of(HotbarFixtures.PLAYER)).isEqualTo(full);
        assertThat(sent).containsExactly("§cEste slot da hotbar já tem o máximo de entradas.");
        assertThat(opened).isEmpty();
    }

    @Test
    void theRemoveMenuListsTheEntriesOfTheSlotWithTheirNamesAndDescriptions() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(3, BLOCKS).add(3, WOOL).add(3, PICKAXE));

        Menu menu = editor.removing(HotbarFixtures.PLAYER, 3, back);

        assertThat(menu.title()).isEqualTo("Removendo do slot #4...");
        assertThat(menu.rows()).isEqualTo(4);
        assertThat(icon(menu, 10).material()).isEqualTo(Material.HARD_CLAY);
        assertThat(icon(menu, 10).name()).isEqualTo("§aBlocos");
        assertThat(icon(menu, 10).lore()).containsExactly(" ", "§7Incluí todos os itens desta categoria.", " ", "§eClique para remover.");
        assertThat(icon(menu, 11).material()).isEqualTo(Material.WOOL);
        assertThat(icon(menu, 11).amount()).isEqualTo(16);
        assertThat(icon(menu, 11).name()).isEqualTo("§aLã");
        assertThat(icon(menu, 11).lore()).containsExactly(" ", "§7Ótimo para fazer pontes.", " ", "§eClique para remover.");
        assertThat(icon(menu, 12).material()).isEqualTo(Material.DIAMOND_PICKAXE);
        assertThat(icon(menu, 12).lore()).containsExactly(" ", "§eClique para remover.");
        assertThat(menu.button(13)).isEmpty();
        assertThat(icon(menu, 30).name()).isEqualTo("§aVoltar");
        assertThat(icon(menu, 30).lore()).containsExactly("§7Para Editando hotbar...");
    }

    @Test
    void removingAnEntryKeepsTheRemoveMenuOpenWhileEntriesRemain() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(3, BLOCKS).add(3, WOOL));

        HotbarFixtures.click(editor.removing(HotbarFixtures.PLAYER, 3, back), 10, ClickType.LEFT, player);

        assertThat(store.of(HotbarFixtures.PLAYER).entries(3)).containsExactly(WOOL);
        assertThat(lastOpened().title()).isEqualTo("Removendo do slot #4...");
        assertThat(icon(lastOpened(), 10).name()).isEqualTo("§aLã");
    }

    @Test
    void removingTheLastEntryReturnsToTheEditor() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(3, WOOL));

        HotbarFixtures.click(editor.removing(HotbarFixtures.PLAYER, 3, back), 10, ClickType.LEFT, player);

        assertThat(store.of(HotbarFixtures.PLAYER).isEmpty()).isTrue();
        assertThat(lastOpened().title()).isEqualTo("Editando hotbar...");
    }

    @Test
    void voltarOnTheRemoveMenuReturnsToTheEditor() {
        store.change(HotbarFixtures.PLAYER, HotbarLayout.empty().add(3, WOOL));

        HotbarFixtures.click(editor.removing(HotbarFixtures.PLAYER, 3, back), 30, ClickType.LEFT, player);

        assertThat(lastOpened().title()).isEqualTo("Editando hotbar...");
        assertThat(store.saves()).hasSize(1);
    }
}
