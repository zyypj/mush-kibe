package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.ClickContext;
import br.com.muxe.core.bukkit.menu.Menu;
import br.com.muxe.core.bukkit.menu.MenuClick;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.language.domain.Localized;
import br.com.muxe.core.common.permission.domain.HeldRank;

public final class AccountMenu {

    public static final String SOON = "account.soon-click";

    private final Localized<AccountSettings> settings;
    private final AccountLines lines;
    private final MessageCatalog messages;

    public AccountMenu(AccountSettings settings, AccountLines lines, MessageCatalog messages) {
        this(Localized.same(settings), lines, messages);
    }

    public AccountMenu(Localized<AccountSettings> settings, AccountLines lines, MessageCatalog messages) {
        this.settings = settings;
        this.lines = lines;
        this.messages = messages;
    }

    public Menu menu(AccountProfile profile, boolean staff) {
        return menu(Language.DEFAULT, profile, staff);
    }

    public Menu menu(Language language, AccountProfile profile, boolean staff) {
        AccountSettings current = settings.in(language);
        Account account = profile.details().account();
        Menu.Builder builder = Menu.builder(current.title().replace("{player}", profile.name())).rows(current.rows());
        String[] placeholders = {
            "player", profile.name(),
            "name", profile.display().formatName(profile.name()),
            "colored-name", profile.display().coloredName(profile.name()),
            "type", lines.type(language, account),
            "number", Long.toString(profile.details().number()),
            "profile-id", messages.get(language, "account.soon"),
            "uuid", account.id().toString(),
            "first-login", current.menuDates().format(account.firstSeenAt()),
            "last-login", current.menuDates().format(account.lastSeenAt()),
            "discord", messages.get(language, "account.soon")
        };
        ItemBuilder head = current.head().item().skullOwner(profile.name());
        for (HeldRank rank : profile.ranks()) {
            head = head.addLore(current.rankLine().replace("{rank}", rank.display()));
        }
        head = head.addLore(current.headFooter().toArray(String[]::new)).replacing(placeholders);
        builder.button(current.head().slot(), head);
        builder.button(current.close().slot(), current.close().item(), ClickContext::close);
        if (staff) {
            MenuClick soon = context -> context.player().sendMessage(messages.get(context.player(), SOON));
            for (ConfiguredButton button : current.staffButtons()) {
                builder.button(button.slot(), button.item(), soon);
            }
        }
        return builder.build();
    }
}
