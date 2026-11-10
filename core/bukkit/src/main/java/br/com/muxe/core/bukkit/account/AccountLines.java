package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.language.domain.Language;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.RemainingTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.command.CommandSender;

public final class AccountLines {

    private static final String PREFIX = "account.";

    private final MessageCatalog messages;
    private final DateTimeFormatter dates;

    public AccountLines(MessageCatalog messages, DateTimeFormatter dates) {
        this.messages = messages;
        this.dates = dates;
    }

    public List<String> lines(AccountProfile profile) {
        return lines(Language.DEFAULT, profile);
    }

    public List<String> lines(CommandSender viewer, AccountProfile profile) {
        return lines(messages.language(viewer), profile);
    }

    public List<String> lines(Language language, AccountProfile profile) {
        Account account = profile.details().account();
        List<String> lines = new ArrayList<>();
        add(language, lines, "user", "player", profile.name());
        add(language, lines, "type", "type", type(language, account));
        add(language, lines, "discord", "discord", messages.get(language, PREFIX + "soon"));
        for (HeldRank rank : profile.ranks()) {
            add(language, lines, "rank", "rank", rank.name(), "rank-display", Colors.translate(rank.display()));
            if (rank.group().id().equals(Group.DEFAULT)) {
                continue;
            }
            add(language, lines, "rank-added", "date", dates.format(rank.node().addedAt()));
            add(language, lines, "rank-updated", "date", dates.format(rank.node().updatedAt()));
            rank.node().expiresAt().ifPresent(at -> add(language, lines, "rank-expires",
                "date", dates.format(at), "time", RemainingTime.format(Duration.between(profile.now(), at))));
        }
        add(language, lines, "first-login", "date", dates.format(account.firstSeenAt()));
        add(language, lines, "last-login", "date", dates.format(account.lastSeenAt()));
        add(language, lines, "location");
        AccountLocation location = profile.details().location().orElse(AccountLocation.UNKNOWN);
        if (location.isUnknown()) {
            add(language, lines, "location-unknown");
        } else {
            place(language, lines, "country", location.country());
            place(language, lines, "region", location.region());
            place(language, lines, "city", location.city());
            place(language, lines, "asn", location.asn());
        }
        return lines;
    }

    public String type(Account account) {
        return type(Language.DEFAULT, account);
    }

    public String type(Language language, Account account) {
        return messages.get(language, PREFIX + (account.premium() ? "premium" : "cracked"));
    }

    private void place(Language language, List<String> lines, String key, Optional<String> value) {
        value.ifPresent(found -> add(language, lines, key, "value", found));
    }

    private void add(Language language, List<String> lines, String key, String... placeholders) {
        lines.addAll(messages.lines(language, PREFIX + key, (Object[]) placeholders));
    }
}
