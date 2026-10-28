package br.com.muxe.core.bukkit.permission.display;

import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Tag;
import java.util.Objects;
import java.util.Optional;

public record RankDisplay(Optional<Tag> tag) {

    public static final String DEFAULT_COLOR = "&7";
    public static final String DEFAULT_TAG = "&7Membro";
    public static final RankDisplay NONE = new RankDisplay(Optional.empty());

    public RankDisplay {
        Objects.requireNonNull(tag, "tag");
    }

    public static RankDisplay of(PermissionData data, ResolvedPermissions resolved, Optional<String> selected) {
        Optional<Tag> chosen = selected.flatMap(data::tag).filter(resolved::canUse);
        if (chosen.isEmpty()) {
            chosen = resolved.primaryGroup().flatMap(Group::tag).flatMap(data::tag).filter(resolved::canUse);
        }
        return new RankDisplay(chosen.isPresent() ? chosen : resolved.displayTag(Optional.empty()));
    }

    public String prefix() {
        return tag.map(Tag::chatPrefix).orElse("");
    }

    public String color() {
        return tag.map(Tag::color).orElse(DEFAULT_COLOR);
    }

    public String formatName(String player) {
        return prefix() + color() + player;
    }

    public String coloredName(String player) {
        return color() + player;
    }

    public String tagDisplay() {
        return tag.map(Tag::display).orElse(DEFAULT_TAG);
    }

    public String chatFormat(String template) {
        return Colors.translate(escape(template
            .replace("{prefix}", prefix())
            .replace("{color}", color())))
            .replace("{player}", "%1$s")
            .replace("{message}", "%2$s");
    }

    public String preview(String template, String player, String message) {
        return template
            .replace("{prefix}", prefix())
            .replace("{color}", color())
            .replace("{player}", player)
            .replace("{message}", message);
    }

    private static String escape(String text) {
        return text.replace("%", "%%");
    }
}
