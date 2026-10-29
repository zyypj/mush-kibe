package br.com.muxe.core.bukkit.permission.command;

import br.com.muxe.core.bukkit.permission.display.RankDisplay;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.permission.domain.Contexts;
import br.com.muxe.core.common.permission.domain.Group;
import br.com.muxe.core.common.permission.domain.HeldRank;
import br.com.muxe.core.common.permission.domain.PermissionData;
import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.PermissionResolver;
import br.com.muxe.core.common.permission.domain.PermissionUser;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import br.com.muxe.core.common.permission.domain.Track;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class RankReports {

    private final MessageCatalog messages;
    private final Supplier<PermissionData> data;
    private final Clock clock;
    private final DateTimeFormatter dates;
    private final NodeText nodes;

    public RankReports(MessageCatalog messages, Supplier<PermissionData> data, Clock clock, DateTimeFormatter dates) {
        this.messages = messages;
        this.data = data;
        this.clock = clock;
        this.dates = dates;
        this.nodes = new NodeText(messages);
    }

    public List<String> player(String name, PermissionUser user) {
        PermissionData current = data.get();
        Instant now = clock.instant();
        ResolvedPermissions resolved = PermissionResolver.resolve(current, user, Contexts.NONE, now);
        List<String> lines = new ArrayList<>(messages.lines("rank-view-header", "player", name));
        String primary = resolved.primaryGroup().map(group -> NodeText.group(current, group)).orElse(Group.DEFAULT);
        lines.addAll(messages.lines("rank-view-primary", "rank", primary));
        lines.addAll(messages.lines("rank-view-tag", "tag", Colors.translate(RankDisplay.of(current, resolved, user.selectedTag()).tagDisplay())));
        List<HeldRank> held = HeldRank.of(current, user, now);
        if (held.isEmpty()) {
            String member = current.group(Group.DEFAULT).map(group -> NodeText.group(current, group)).orElse(Group.DEFAULT);
            lines.addAll(messages.lines("rank-view-none", "rank", member));
        }
        for (HeldRank rank : held) {
            lines.addAll(messages.lines("rank-view-line",
                "rank", Colors.translate(rank.display()),
                "time", RankText.remaining(messages, rank.node().expiresAt(), now),
                "date", dates.format(rank.node().addedAt())));
        }
        return lines;
    }

    public List<String> ranks() {
        PermissionData current = data.get();
        List<String> lines = new ArrayList<>(messages.lines("rank-list-header"));
        for (Group group : current.groupsByWeight()) {
            lines.addAll(messages.lines("rank-list-line",
                "rank", NodeText.group(current, group),
                "id", group.id(),
                "weight", Integer.toString(group.weight()),
                "parents", parents(current, group)));
        }
        lines.addAll(messages.lines("rank-list-tracks"));
        current.tracks().values().stream().sorted(Comparator.comparing(Track::id)).forEach(track -> lines.addAll(messages.lines("rank-list-track",
            "track", track.id(),
            "groups", track.groups().stream()
                .map(id -> current.group(id).map(group -> NodeText.group(current, group)).orElse(id))
                .collect(Collectors.joining(messages.get("rank-list-track-separator"))))));
        return lines;
    }

    public List<String> rank(Group group) {
        PermissionData current = data.get();
        List<String> lines = new ArrayList<>(messages.lines("rank-info-header", "rank", NodeText.group(current, group), "id", group.id()));
        lines.addAll(messages.lines("rank-info-weight", "weight", Integer.toString(group.weight())));
        group.tag().flatMap(current::tag).ifPresent(tag -> lines.addAll(messages.lines("rank-info-tag",
            "tag", Colors.translate(tag.display()), "prefix", Colors.translate(tag.formatName("Steve")))));
        lines.addAll(messages.lines("rank-info-parents", "parents", parents(current, group)));
        lines.addAll(nodeLines("rank-info-nodes", group.nodes().stream().filter(node -> !node.isGroup()).toList()));
        return lines;
    }

    public List<String> userNodes(String name, PermissionUser user) {
        List<String> lines = new ArrayList<>(messages.lines("perm-user-header", "player", name, "count", Integer.toString(user.nodes().size())));
        lines.addAll(nodeLines(null, user.nodes()));
        return lines;
    }

    private List<String> nodeLines(String headerKey, List<PermissionNode> list) {
        List<String> lines = new ArrayList<>();
        if (headerKey != null) {
            lines.addAll(messages.lines(headerKey, "count", Integer.toString(list.size())));
        }
        if (list.isEmpty()) {
            lines.addAll(messages.lines("node-none"));
        }
        Instant now = clock.instant();
        list.stream()
            .sorted(Comparator.comparing(PermissionNode::key).thenComparing(node -> node.contexts().encode()))
            .forEach(node -> lines.add(nodes.line(node, now)));
        return lines;
    }

    private String parents(PermissionData current, Group group) {
        if (group.parents().isEmpty()) {
            return messages.get("rank-nothing");
        }
        return group.parents().stream()
            .map(id -> current.group(id).map(parent -> NodeText.group(current, parent)).orElse(id))
            .collect(Collectors.joining(messages.get("rank-list-separator")));
    }
}
