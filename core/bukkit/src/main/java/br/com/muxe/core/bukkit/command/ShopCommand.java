package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.shop.Shop;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.shop.domain.Promotion;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShopCommand implements CommandExecutor {

    public static final String NAME = "loja";
    public static final String PERMISSION = "muxe.shop.manage";

    private final Optional<Shop> shop;
    private final PlacementSync placements;
    private final MessageCatalog messages;
    private final AsyncReply reply;

    public ShopCommand(Optional<Shop> shop, PlacementSync placements, MessageCatalog messages, AsyncReply reply) {
        this.shop = shop;
        this.placements = placements;
        this.messages = messages;
        this.reply = reply;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("players-only"));
            return true;
        }
        if (shop.isEmpty()) {
            player.sendMessage(messages.get("shop-unavailable"));
            return true;
        }
        if (args.length == 0) {
            shop.get().service().open(player);
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(messages.get("no-permission"));
            return true;
        }
        switch (CommandText.word(args, 0)) {
            case "caixa" -> register(player, args);
            case "vendedor" -> seller(player, args);
            case "promocao" -> promotion(player, args, shop.get());
            default -> usage(player);
        }
        return true;
    }

    private void register(Player player, String[] args) {
        switch (CommandText.word(args, 1)) {
            case "criar" -> createRegister(player, args);
            case "mover" -> withRegister(player, args, register -> reply.then(player, placements.save(register.movedTo(Positions.standing(player.getLocation()))),
                saved -> player.sendMessage(messages.get("shop-register-moved", "id", register.id()))));
            case "remover" -> withRegister(player, args, register -> reply.then(player, placements.remove(PlacementKind.REGISTER, register.id()),
                removed -> player.sendMessage(messages.get("shop-register-removed", "id", register.id()))));
            case "listar" -> listRegisters(player);
            default -> usage(player);
        }
    }

    private void createRegister(Player player, String[] args) {
        String id = CommandText.word(args, 2);
        if (!PlacementIds.isValid(id)) {
            player.sendMessage(messages.get("shop-register-invalid-id"));
            return;
        }
        if (placements.current().register(id).isPresent()) {
            player.sendMessage(messages.get("shop-register-exists", "id", id));
            return;
        }
        Position here = Positions.standing(player.getLocation());
        reply.then(player, placements.save(new RegisterPlacement(placements.group(), id, here, Optional.of(here))),
            saved -> player.sendMessage(messages.get("shop-register-created", "id", id)));
    }

    private void seller(Player player, String[] args) {
        if (args.length < 2) {
            usage(player);
            return;
        }
        String id = CommandText.word(args, 1);
        Optional<RegisterPlacement> register = placements.current().register(id);
        if (register.isEmpty()) {
            player.sendMessage(messages.get("shop-register-unknown", "id", id));
            return;
        }
        if (CommandText.word(args, 2).equals("remover")) {
            reply.then(player, placements.save(register.get().withSeller(Optional.empty())),
                saved -> player.sendMessage(messages.get("shop-seller-removed", "id", id)));
            return;
        }
        reply.then(player, placements.save(register.get().withSeller(Optional.of(Positions.standing(player.getLocation())))),
            saved -> player.sendMessage(messages.get("shop-seller-moved", "id", id)));
    }

    private void promotion(Player player, String[] args, Shop current) {
        Promotion promotion = current.promotion().current();
        switch (CommandText.word(args, 1)) {
            case "" -> showPromotion(player, promotion);
            case "produto" -> {
                String product = CommandText.word(args, 2);
                if (current.service().catalog().featurable(product).isEmpty()) {
                    player.sendMessage(messages.get("shop-promotion-product-unknown", "product", product));
                    return;
                }
                reply.then(player, current.promotion().change(promotion.featuring(product)),
                    saved -> player.sendMessage(messages.get("shop-promotion-product-changed", "product", product)));
            }
            case "adicionar" -> {
                if (args.length < 3) {
                    usage(player);
                    return;
                }
                reply.then(player, current.promotion().change(promotion.adding(CommandText.line(args, 2))),
                    saved -> player.sendMessage(messages.get("shop-promotion-line-added")));
            }
            case "linha" -> withPromotionLine(player, args, promotion, 4, number -> reply.then(player,
                current.promotion().change(promotion.replacing(number, CommandText.line(args, 3))),
                saved -> player.sendMessage(messages.get("shop-promotion-line-changed", "line", Integer.toString(number)))));
            case "removerlinha" -> withPromotionLine(player, args, promotion, 3, number -> reply.then(player,
                current.promotion().change(promotion.without(number)),
                saved -> player.sendMessage(messages.get("shop-promotion-line-removed", "line", Integer.toString(number)))));
            default -> usage(player);
        }
    }

    private void showPromotion(Player player, Promotion promotion) {
        player.sendMessage(messages.get("shop-promotion-header", "product", promotion.productId()));
        if (promotion.lines().isEmpty()) {
            player.sendMessage(messages.get("shop-promotion-no-lines"));
        }
        for (int index = 0; index < promotion.lines().size(); index++) {
            String line = promotion.lines().get(index);
            player.sendMessage(messages.get("shop-promotion-line", "line", Integer.toString(index + 1), "text", line.isEmpty() ? CommandText.EMPTY_LINE : Colors.translate(line)));
        }
    }

    private void withPromotionLine(Player player, String[] args, Promotion promotion, int minimumArgs, IntConsumer action) {
        if (args.length < minimumArgs) {
            usage(player);
            return;
        }
        OptionalInt number = CommandText.number(args[2]);
        if (number.isEmpty() || !promotion.hasLine(number.getAsInt())) {
            player.sendMessage(messages.get("shop-promotion-invalid-line", "count", Integer.toString(promotion.lines().size())));
            return;
        }
        action.accept(number.getAsInt());
    }

    private void listRegisters(Player player) {
        List<RegisterPlacement> all = placements.current().registers();
        if (all.isEmpty()) {
            player.sendMessage(messages.get("shop-register-list-empty"));
            return;
        }
        player.sendMessage(messages.get("shop-register-list-header", "count", Integer.toString(all.size()), "group", placements.group()));
        for (RegisterPlacement register : all) {
            player.sendMessage(messages.get("shop-register-list-line",
                "id", register.id(),
                "location", register.position().describe(),
                "seller", register.seller().map(Position::describe).orElse("-")));
        }
    }

    private void withRegister(Player player, String[] args, Consumer<RegisterPlacement> action) {
        String id = CommandText.word(args, 2);
        Optional<RegisterPlacement> register = placements.current().register(id);
        if (register.isEmpty()) {
            player.sendMessage(messages.get("shop-register-unknown", "id", id));
            return;
        }
        action.accept(register.get());
    }

    private void usage(Player player) {
        messages.lines("shop-usage").forEach(player::sendMessage);
    }
}
