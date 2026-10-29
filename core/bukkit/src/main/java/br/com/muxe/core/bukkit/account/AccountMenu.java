package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuClick;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.permission.domain.HeldRank;

public final class AccountMenu {

    public static final String SOON = "account.soon-click";

    private final AccountSettings settings;
    private final AccountLines lines;
    private final MessageCatalog messages;

    public AccountMenu(AccountSettings settings, AccountLines lines, MessageCatalog messages) {
        this.settings = settings;
        this.lines = lines;
        this.messages = messages;
    }

    public Menu menu(AccountProfile profile, boolean staff) {
        Account account = profile.details().account();
        Menu.Builder builder = Menu.builder(settings.title().replace("{player}", profile.name())).rows(settings.rows());
        String[] placeholders = {
            "player", profile.name(),
            "name", profile.display().formatName(profile.name()),
            "colored-name", profile.display().coloredName(profile.name()),
            "type", lines.type(account),
            "number", Long.toString(profile.details().number()),
            "profile-id", messages.get("account.soon"),
            "uuid", account.id().toString(),
            "first-login", settings.menuDates().format(account.firstSeenAt()),
            "last-login", settings.menuDates().format(account.lastSeenAt()),
            "discord", messages.get("account.soon")
        };
        ItemBuilder head = settings.head().item().skullOwner(profile.name());
        for (HeldRank rank : profile.ranks()) {
            head = head.addLore(settings.rankLine().replace("{rank}", rank.display()));
        }
        head = head.addLore(settings.headFooter().toArray(String[]::new)).replacing(placeholders);
        builder.button(settings.head().slot(), head);
        builder.button(settings.close().slot(), settings.close().item(), ClickContext::close);
        if (staff) {
            MenuClick soon = context -> context.player().sendMessage(messages.get(SOON));
            for (ConfiguredButton button : settings.staffButtons()) {
                builder.button(button.slot(), button.item(), soon);
            }
        }
        return builder.build();
    }
}
