package br.com.muxe.core.bukkit.customroom.menu;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.prompt.AnvilEdits;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.customroom.domain.RoomOptions;
import br.com.muxe.core.common.customroom.schema.OptionEntry;
import br.com.muxe.core.common.customroom.schema.OptionEntry.ShopItem;
import br.com.muxe.core.common.customroom.schema.OptionEntry.Upgrade;
import br.com.muxe.core.common.customroom.schema.OptionPage;
import br.com.muxe.core.common.customroom.schema.RoomEditor;
import br.com.muxe.core.common.customroom.schema.RoomEditor.Click;
import br.com.muxe.core.common.customroom.schema.RoomEditor.Outcome;
import br.com.muxe.core.common.customroom.schema.RoomEditor.Prompt;
import br.com.muxe.core.common.customroom.schema.RoomEditor.ShopField;
import br.com.muxe.core.common.customroom.schema.RoomLore;
import br.com.muxe.core.common.customroom.schema.RoomLore.Shown;
import br.com.muxe.core.common.customroom.schema.RoomTexts;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

public final class OptionMenus {

    private final AnvilEdits anvils;
    private final Consumer<Player> invalidValue;

    public OptionMenus(AnvilEdits anvils, Consumer<Player> invalidValue) {
        this.anvils = anvils;
        this.invalidValue = invalidValue;
    }

    public void open(Player player, OptionsTarget target, String pageId) {
        RoomLore lore = target.lore();
        Optional<OptionPage> found = lore.editor().schema().page(pageId);
        if (found.isEmpty()) {
            return;
        }
        OptionPage page = found.get();
        RoomTexts texts = lore.texts();
        RoomOptions options = target.options();
        Menu.Builder menu = Menu.builder(Colors.translate(trim(page.title()))).rows(page.rows());
        for (OptionEntry entry : page.entries()) {
            if (entry instanceof OptionEntry.Team team && team.index() > lore.editor().teamCount(options)) {
                continue;
            }
            menu.button(entry.slot(), RoomIcons.item(lore.entry(entry, options)), context -> clicked(context, target, page, entry));
        }
        page.previous().ifPresent(turn -> menu.button(turn.slot(), pageButton(texts, turn.number()), context -> open(context.player(), target, turn.page())));
        page.next().ifPresent(turn -> menu.button(turn.slot(), pageButton(texts, turn.number()), context -> open(context.player(), target, turn.page())));
        boolean backTaken = page.previous().map(turn -> turn.slot() == page.backSlot()).orElse(false);
        if (page.backSlot() >= 0 && !backTaken) {
            String title = page.backTo().isBlank() ? target.parentTitle() : lore.editor().schema().page(page.backTo()).map(OptionPage::title).orElse("");
            menu.button(page.backSlot(), backButton(texts, title), context -> {
                if (page.backTo().isBlank()) {
                    target.back(context.player());
                } else {
                    open(context.player(), target, page.backTo());
                }
            });
        }
        menu.build().open(player);
    }

    public void openShop(Player player, OptionsTarget target, String fromPage, ShopItem item) {
        RoomLore lore = target.lore();
        RoomTexts texts = lore.texts();
        Map<Integer, Shown> shown = lore.shopEditor(item, target.options());
        Menu.Builder menu = Menu.builder(Colors.translate(trim(texts.get("editing-title", "name", item.name())))).rows(3);
        menu.button(RoomLore.SHOP_STATUS_SLOT, RoomIcons.item(shown.get(RoomLore.SHOP_STATUS_SLOT)),
            context -> shopClicked(context, target, fromPage, item, ShopField.STATUS));
        menu.button(RoomLore.SHOP_PRICE_SLOT, RoomIcons.item(shown.get(RoomLore.SHOP_PRICE_SLOT)),
            context -> shopClicked(context, target, fromPage, item, ShopField.PRICE));
        menu.button(RoomLore.SHOP_LIMIT_SLOT, RoomIcons.item(shown.get(RoomLore.SHOP_LIMIT_SLOT)),
            context -> shopClicked(context, target, fromPage, item, ShopField.LIMIT));
        String parent = lore.editor().schema().page(fromPage).map(OptionPage::title).orElse("");
        menu.button(RoomLore.SHOP_BACK_SLOT, backButton(texts, parent), context -> open(context.player(), target, fromPage));
        menu.build().open(player);
    }

    public void openUpgrade(Player player, OptionsTarget target, String fromPage, Upgrade upgrade) {
        RoomLore lore = target.lore();
        RoomTexts texts = lore.texts();
        Map<Integer, Shown> shown = lore.upgradeEditor(upgrade, target.options());
        Menu.Builder menu = Menu.builder(Colors.translate(trim(texts.get("editing-title", "name", upgrade.name())))).rows(3);
        shown.forEach((slot, icon) -> {
            int level = slot == RoomLore.UPGRADE_STATUS_SLOT ? 0 : slot - RoomLore.UPGRADE_FIRST_LEVEL_SLOT + 1;
            menu.button(slot, RoomIcons.item(icon), context -> upgradeClicked(context, target, fromPage, upgrade, level));
        });
        String parent = lore.editor().schema().page(fromPage).map(OptionPage::title).orElse("");
        menu.button(RoomLore.UPGRADE_BACK_SLOT, backButton(texts, parent), context -> open(context.player(), target, fromPage));
        menu.build().open(player);
    }

    public static Click click(ClickType type) {
        return switch (type) {
            case DROP, CONTROL_DROP -> Click.DROP;
            case SHIFT_LEFT -> Click.SHIFT_LEFT;
            case SHIFT_RIGHT -> Click.SHIFT_RIGHT;
            case RIGHT -> Click.RIGHT;
            default -> Click.LEFT;
        };
    }

    public static ItemBuilder backButton(RoomTexts texts, String title) {
        return ItemBuilder.of(Material.ARROW).name(Colors.translate(texts.get("back-name")))
            .lore(List.of(Colors.translate(texts.get("back-lore", "title", plainTitle(title)))));
    }

    public static String plainTitle(String title) {
        return title.replaceAll(" \\(\\d+/\\d+\\)$", "");
    }

    private static ItemBuilder pageButton(RoomTexts texts, int number) {
        return ItemBuilder.of(Material.ARROW).name(Colors.translate(texts.get("page-name", "page", String.valueOf(number))));
    }

    private void clicked(ClickContext context, OptionsTarget target, OptionPage page, OptionEntry entry) {
        RoomEditor editor = target.lore().editor();
        Outcome outcome = editor.click(entry, click(context.type()), target.options());
        handle(context.player(), target, outcome, () -> open(context.player(), target, page.id()), page.id());
    }

    private void shopClicked(ClickContext context, OptionsTarget target, String fromPage, ShopItem item, ShopField field) {
        Outcome outcome = target.lore().editor().shopClick(item, field, click(context.type()), target.options());
        handle(context.player(), target, outcome, () -> openShop(context.player(), target, fromPage, item), fromPage);
    }

    private void upgradeClicked(ClickContext context, OptionsTarget target, String fromPage, Upgrade upgrade, int level) {
        Outcome outcome = target.lore().editor().upgradeClick(upgrade, level, click(context.type()), target.options());
        handle(context.player(), target, outcome, () -> openUpgrade(context.player(), target, fromPage, upgrade), fromPage);
    }

    private void handle(Player player, OptionsTarget target, Outcome outcome, Runnable reopen, String page) {
        switch (outcome) {
            case Outcome.Changed changed -> {
                target.change(player, changed.options());
                reopen.run();
            }
            case Outcome.Open open -> open(player, target, open.page());
            case Outcome.EditShop shop -> openShop(player, target, page, shop.item());
            case Outcome.EditUpgrade upgrade -> openUpgrade(player, target, page, upgrade.upgrade());
            case Outcome.Ask ask -> ask(player, target, ask.prompt(), reopen);
            case Outcome.Nothing nothing -> {
            }
        }
    }

    private void ask(Player player, OptionsTarget target, Prompt prompt, Runnable reopen) {
        RoomTexts texts = target.lore().texts();
        String current = prompt.current().isBlank() ? " " : prompt.current();
        anvils.edit(player, current, typed -> prompt.label().isBlank()
                ? texts.get("anvil-result-plain", "value", typed)
                : texts.get("anvil-result", "label", prompt.label(), "value", typed),
            typed -> {
                Optional<RoomOptions> answered = target.lore().editor().answer(prompt, typed, target.options());
                if (answered.isPresent()) {
                    target.change(player, answered.get());
                } else {
                    invalidValue.accept(player);
                }
                reopen.run();
            }, reopen);
    }

    private static String trim(String title) {
        return title.length() > Menu.MAX_TITLE ? title.substring(0, Menu.MAX_TITLE) : title;
    }
}
