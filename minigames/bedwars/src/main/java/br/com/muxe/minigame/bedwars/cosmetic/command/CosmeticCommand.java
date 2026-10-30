package br.com.muxe.minigame.bedwars.cosmetic.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.permission.command.PermissionSuggestions;
import br.com.muxe.core.bukkit.permission.command.Replies;
import br.com.muxe.core.bukkit.permission.command.TargetLookup;
import br.com.muxe.core.common.cosmetic.application.command.GrantCosmeticCommand;
import br.com.muxe.core.common.cqrs.command.CommandBus;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.BedCosmetic;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.CosmeticCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenus;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticProfileListener;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = CosmeticCommand.NAME, aliases = "cosmeticosbw", description = "Opens the BedWars cosmetics and grants them.")
public final class CosmeticCommand {

    public static final String NAME = "cosmeticos";
    public static final String ADMIN_PERMISSION = "muxe.cosmetics.admin";
    public static final String SOURCE = "staff";

    private static final String COSMETICS = "bed-cosmetics";

    private final CosmeticCatalog catalog;
    private final CosmeticMenus menus;
    private final CommandBus commands;
    private final TargetLookup targets;
    private final Replies replies;
    private final CosmeticProfileListener profiles;

    public CosmeticCommand(CosmeticCatalog catalog, CosmeticMenus menus, CommandBus commands, TargetLookup targets, Replies replies, CosmeticProfileListener profiles) {
        this.catalog = catalog;
        this.menus = menus;
        this.commands = commands;
        this.targets = targets;
        this.replies = replies;
        this.profiles = profiles;
    }

    @Default
    public void open(Player player) {
        menus.openCategories(player);
    }

    @Subcommand("dar")
    @Permission(ADMIN_PERMISSION)
    public void grant(
        CommandSender sender,
        @Arg("jogador") @Suggest(provider = PermissionSuggestions.PLAYERS) String name,
        @Arg("cosmetico") @Suggest(provider = COSMETICS) String id
    ) {
        Optional<BedCosmetic> cosmetic = catalog.find(id.toLowerCase(Locale.ROOT));
        if (cosmetic.isEmpty()) {
            replies.send(sender, "cosmetic-unknown", "id", id);
            return;
        }
        replies.then(sender, targets.find(name), target -> {
            if (target.isEmpty()) {
                replies.send(sender, "player-unknown", "player", name);
                return;
            }
            GrantCosmeticCommand grant = new GrantCosmeticCommand(target.get().id(), catalog.category(), cosmetic.get().id(), SOURCE);
            replies.then(sender, commands.dispatch(grant), result -> {
                boolean added = result.isSuccess() && Boolean.TRUE.equals(result.orElseThrow());
                replies.send(sender, added ? "cosmetic-granted" : "cosmetic-already-had", "player", target.get().name(), "name", cosmetic.get().name());
                Optional.ofNullable(Bukkit.getPlayer(target.get().id())).ifPresent(profiles::load);
            });
        });
    }

    @Suggestions(COSMETICS)
    public List<String> cosmetics() {
        return catalog.all().stream().map(BedCosmetic::id).toList();
    }
}
