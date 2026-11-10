package br.com.muxe.core.bukkit.command.config;

import br.com.muxe.core.bukkit.config.edit.ConfigEditor.ReloadOutcome;
import br.com.muxe.core.bukkit.config.edit.ConfigOutcome;
import br.com.muxe.core.bukkit.config.edit.ConfigView;
import br.com.muxe.core.bukkit.config.edit.ReloadReport;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.List;
import org.bukkit.command.CommandSender;

public final class ConfigReplies {

    public static final int LIST_LIMIT = 40;
    public static final String ROOT = "(raiz)";

    private final MessageCatalog messages;

    public ConfigReplies(MessageCatalog messages) {
        this.messages = messages;
    }

    public MessageCatalog messages() {
        return messages;
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        messages.lines(sender, key, (Object[]) placeholders).forEach(sender::sendMessage);
    }

    public boolean refused(CommandSender sender, ConfigOutcome outcome) {
        switch (outcome.status()) {
            case CHANGED -> {
                return false;
            }
            case INVALID_PATH -> send(sender, "config.invalid-path", "path", outcome.path());
            case PROTECTED -> send(sender, "config.protected", "path", outcome.path());
            case UNKNOWN_PATH -> send(sender, "config.unknown-path", "path", outcome.path());
            case IS_SECTION -> send(sender, "config.is-section", "path", outcome.path());
            case BLOCKED -> send(sender, "config.blocked", "path", outcome.path(), "blocker", outcome.detail());
            case TYPE_MISMATCH -> send(sender, "config.type-mismatch", "path", outcome.path(), "type", outcome.detail());
            case NOT_A_LIST -> send(sender, "config.not-list", "path", outcome.path());
            case MISSING_ITEM -> send(sender, "config.missing-item", "path", outcome.path(), "value", outcome.detail());
        }
        return true;
    }

    public void applied(CommandSender sender, ConfigOutcome outcome) {
        ReloadReport report = outcome.reload();
        if (!report.applied().isEmpty()) {
            send(sender, "config.live", "components", String.join(", ", report.applied()));
        }
        report.failed().forEach((component, reason) -> send(sender, "config.live-failed", "component", component, "reason", reason));
        if (!report.live()) {
            send(sender, "config.restart");
        }
    }

    public void view(CommandSender sender, ConfigView view) {
        switch (view) {
            case ConfigView.Missing missing -> send(sender, "config.view-missing", "path", missing.path());
            case ConfigView.Hidden hidden -> send(sender, "config.protected", "path", hidden.path());
            case ConfigView.Scalar scalar -> send(sender, "config.view-scalar", "path", scalar.path(), "value", scalar.value(), "type", scalar.type());
            case ConfigView.Items items -> {
                send(sender, "config.view-list-header", "path", items.path(), "count", Integer.toString(items.items().size()));
                lines(sender, "config.view-list-line", "item", items.items());
            }
            case ConfigView.Keys keys -> {
                send(sender, "config.view-keys-header", "path", keys.path().isEmpty() ? ROOT : keys.path(), "count", Integer.toString(keys.keys().size()));
                lines(sender, "config.view-keys-line", "key", keys.keys());
            }
        }
    }

    public void reloaded(CommandSender sender, ReloadOutcome outcome) {
        send(sender, "config.reload-done");
        ReloadReport report = outcome.report();
        if (!report.applied().isEmpty()) {
            send(sender, "config.reload-applied", "components", String.join(", ", report.applied()));
        }
        if (!report.covered()) {
            send(sender, "config.reload-nothing");
        }
        report.failed().forEach((component, reason) -> send(sender, "config.live-failed", "component", component, "reason", reason));
        if (!outcome.pendingRestart().isEmpty()) {
            send(sender, "config.reload-pending", "count", Integer.toString(outcome.pendingRestart().size()));
            outcome.pendingRestart().forEach(path -> send(sender, "config.reload-pending-line", "path", path));
        }
    }

    private void lines(CommandSender sender, String key, String name, List<String> values) {
        values.stream().limit(LIST_LIMIT).forEach(value -> send(sender, key, name, value));
        if (values.size() > LIST_LIMIT) {
            send(sender, "config.view-more", "count", Integer.toString(values.size() - LIST_LIMIT));
        }
    }
}
