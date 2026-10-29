package br.com.muxe.core.bukkit.permission;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;

public final class PermissibleInjector {

    private static final String HUMAN_ENTITY = ".entity.CraftHumanEntity";
    private static final String PERMISSIBLE_FIELD = "perm";
    private static final String ATTACHMENTS_FIELD = "attachments";

    private final Optional<Field> permissible;
    private final Optional<Field> attachments;
    private final Logger logger;

    private PermissibleInjector(Optional<Field> permissible, Optional<Field> attachments, Logger logger) {
        this.permissible = permissible;
        this.attachments = attachments;
        this.logger = logger;
    }

    public static PermissibleInjector resolve(Server server, Logger logger) {
        Optional<Field> permissible = Optional.empty();
        Optional<Field> attachments = Optional.empty();
        try {
            Field field = Class.forName(server.getClass().getPackage().getName() + HUMAN_ENTITY).getDeclaredField(PERMISSIBLE_FIELD);
            field.setAccessible(true);
            permissible = Optional.of(field);
            Field list = PermissibleBase.class.getDeclaredField(ATTACHMENTS_FIELD);
            list.setAccessible(true);
            attachments = Optional.of(list);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING, "Permissions fall back to attachments: the player permissible cannot be replaced (" + exception + ")");
        }
        return new PermissibleInjector(permissible, attachments, logger);
    }

    public boolean isAvailable() {
        return permissible.isPresent();
    }

    public Optional<PermissibleBase> inject(Player player, PermissibleBase replacement) {
        if (permissible.isEmpty()) {
            return Optional.empty();
        }
        try {
            PermissibleBase original = (PermissibleBase) permissible.get().get(player);
            copyAttachments(original, replacement);
            permissible.get().set(player, replacement);
            replacement.recalculatePermissions();
            return Optional.of(original);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            logger.log(Level.WARNING, "Could not replace the permissible of " + player.getName(), exception);
            return Optional.empty();
        }
    }

    public void restore(Player player, PermissibleBase original) {
        permissible.ifPresent(field -> {
            try {
                field.set(player, original);
                original.recalculatePermissions();
            } catch (ReflectiveOperationException | RuntimeException exception) {
                logger.log(Level.WARNING, "Could not restore the permissible of " + player.getName(), exception);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void copyAttachments(PermissibleBase from, PermissibleBase to) throws IllegalAccessException {
        if (from == null || attachments.isEmpty()) {
            return;
        }
        List<PermissionAttachment> source = (List<PermissionAttachment>) attachments.get().get(from);
        List<PermissionAttachment> target = (List<PermissionAttachment>) attachments.get().get(to);
        if (source != null && target != null) {
            target.addAll(source);
        }
    }
}
