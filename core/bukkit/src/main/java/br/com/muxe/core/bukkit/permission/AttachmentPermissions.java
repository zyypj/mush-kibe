package br.com.muxe.core.bukkit.permission;

import br.com.muxe.core.common.permission.domain.PermissionNode;
import br.com.muxe.core.common.permission.domain.ResolvedPermissions;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

final class AttachmentPermissions {

    private final Plugin plugin;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    AttachmentPermissions(Plugin plugin) {
        this.plugin = plugin;
    }

    void apply(Player player, ResolvedPermissions resolved) {
        PermissionAttachment attachment = attachments.computeIfAbsent(player.getUniqueId(), ignored -> player.addAttachment(plugin));
        List.copyOf(attachment.getPermissions().keySet()).forEach(attachment::unsetPermission);
        resolved.permissions().forEach((key, value) -> {
            if (!key.contains(PermissionNode.WILDCARD)) {
                attachment.setPermission(key, value);
            }
        });
    }

    void remove(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            attachment.remove();
        }
    }

    void forget(UUID player) {
        attachments.remove(player);
    }
}
