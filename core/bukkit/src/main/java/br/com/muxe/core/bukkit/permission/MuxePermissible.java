package br.com.muxe.core.bukkit.permission;

import br.com.muxe.core.common.permission.domain.Tristate;
import java.util.function.Function;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;

public final class MuxePermissible extends PermissibleBase {

    private final Function<String, Tristate> checker;

    public MuxePermissible(ServerOperator operator, Function<String, Tristate> checker) {
        super(operator);
        this.checker = checker;
    }

    @Override
    public boolean isPermissionSet(String name) {
        return checker.apply(name) != Tristate.UNDEFINED || super.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return checker.apply(permission.getName()) != Tristate.UNDEFINED || super.isPermissionSet(permission);
    }

    @Override
    public boolean hasPermission(String name) {
        Tristate value = checker.apply(name);
        return value == Tristate.UNDEFINED ? super.hasPermission(name) : value.asBoolean();
    }

    @Override
    public boolean hasPermission(Permission permission) {
        Tristate value = checker.apply(permission.getName());
        return value == Tristate.UNDEFINED ? super.hasPermission(permission) : value.asBoolean();
    }
}
