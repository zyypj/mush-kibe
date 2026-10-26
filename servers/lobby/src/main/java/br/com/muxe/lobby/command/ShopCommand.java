package br.com.muxe.lobby.command;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.shop.ShopService;
import br.com.muxe.lobby.shop.register.ShopRegister;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShopCommand implements CommandExecutor {

    public static final String NAME = "loja";
    public static final String PERMISSION = "muxe.lobby.shop";
    private static final String MOVE_REGISTER = "caixa";

    private final ShopService shop;
    private final ShopRegister register;
    private final MessageCatalog messages;

    public ShopCommand(ShopService shop, ShopRegister register, MessageCatalog messages) {
        this.shop = shop;
        this.register = register;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (args.length > 0 && args[0].toLowerCase(Locale.ROOT).equals(MOVE_REGISTER)) {
            if (!player.hasPermission(PERMISSION)) {
                player.sendMessage(messages.get("no-permission"));
                return true;
            }
            register.moveTo(player.getLocation());
            player.sendMessage(messages.get("shop-register-moved"));
            return true;
        }
        shop.open(player);
        return true;
    }
}
