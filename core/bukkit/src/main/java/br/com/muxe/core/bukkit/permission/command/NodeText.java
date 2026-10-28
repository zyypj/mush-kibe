package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.RemainingTime;
import br.com.muxe.core.common.permission.domain.Tag;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

public final class NodeText {

    private static final String NO_TAG_COLOR = "&7";

    private final MessageCatalog messages;

    public NodeText(MessageCatalog messages) {
        this.messages = messages;
    }

    public static String group(PermissionData data, Group group) {
        return Colors.translate(group.tag().flatMap(data::tag).map(Tag::display).orElse(NO_TAG_COLOR + group.displayName()));
    }

    public String line(PermissionNode node, Instant now) {
        return messages.get("node-line",
            "key", node.key(),
            "value", value(node.value()),
            "contexts", contexts(node.contexts()),
            "time", time(node.expiresAt(), now));
    }

    public String value(boolean value) {
        return messages.get(value ? "node-true" : "node-false");
    }

    public String contexts(Contexts contexts) {
        if (contexts.isEmpty()) {
            return "";
        }
        String described = contexts.values().entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .sorted()
            .collect(Collectors.joining(", "));
        return messages.get("node-contexts", "contexts", described);
    }

    public String time(Optional<Instant> expiresAt, Instant now) {
        return expiresAt.map(at -> messages.get("node-time", "time", RemainingTime.format(Duration.between(now, at)))).orElse("");
    }
}
