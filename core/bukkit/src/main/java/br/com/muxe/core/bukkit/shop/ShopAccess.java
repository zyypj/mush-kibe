package br.com.muxe.core.bukkit.shop;

import br.com.muxe.core.bukkit.text.MessageCatalog;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class ShopAccess {

    private final Optional<ShopService> service;
    private final MessageCatalog messages;

    public ShopAccess(Optional<ShopService> service, MessageCatalog messages) {
        this.service = service;
        this.messages = messages;
    }

    public void open(Player player) {
        open(player, "");
    }

    public void open(Player player, String category) {
        if (service.isEmpty()) {
            player.sendMessage(messages.get("shop-unavailable"));
            return;
        }
        ShopService shop = service.get();
        shop.catalog().category(category).ifPresentOrElse(found -> shop.category(player, found), () -> shop.open(player));
    }
}
