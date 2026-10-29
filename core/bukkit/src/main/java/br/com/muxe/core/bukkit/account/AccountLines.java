package br.com.muxe.core.bukkit.account;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.account.domain.Account;
import br.com.muxe.core.common.account.domain.AccountLocation;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.RemainingTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AccountLines {

    private static final String PREFIX = "account.";

    private final MessageCatalog messages;
    private final DateTimeFormatter dates;

    public AccountLines(MessageCatalog messages, DateTimeFormatter dates) {
        this.messages = messages;
        this.dates = dates;
    }

    public List<String> lines(AccountProfile profile) {
        Account account = profile.details().account();
        List<String> lines = new ArrayList<>();
        add(lines, "user", "player", profile.name());
        add(lines, "type", "type", type(account));
        add(lines, "discord", "discord", messages.get(PREFIX + "soon"));
        for (HeldRank rank : profile.ranks()) {
            add(lines, "rank", "rank", rank.name(), "rank-display", Colors.translate(rank.display()));
            if (rank.group().id().equals(Group.DEFAULT)) {
                continue;
            }
            add(lines, "rank-added", "date", dates.format(rank.node().addedAt()));
            add(lines, "rank-updated", "date", dates.format(rank.node().updatedAt()));
            rank.node().expiresAt().ifPresent(at -> add(lines, "rank-expires",
                "date", dates.format(at), "time", RemainingTime.format(Duration.between(profile.now(), at))));
        }
        add(lines, "first-login", "date", dates.format(account.firstSeenAt()));
        add(lines, "last-login", "date", dates.format(account.lastSeenAt()));
        add(lines, "location");
        AccountLocation location = profile.details().location().orElse(AccountLocation.UNKNOWN);
        if (location.isUnknown()) {
            add(lines, "location-unknown");
        } else {
            place(lines, "country", location.country());
            place(lines, "region", location.region());
            place(lines, "city", location.city());
            place(lines, "asn", location.asn());
        }
        return lines;
    }

    public String type(Account account) {
        return messages.get(PREFIX + (account.premium() ? "premium" : "cracked"));
    }

    private void place(List<String> lines, String key, Optional<String> value) {
        value.ifPresent(found -> add(lines, key, "value", found));
    }

    private void add(List<String> lines, String key, String... placeholders) {
        lines.addAll(messages.lines(PREFIX + key, placeholders));
    }
}
