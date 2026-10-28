package br.com.muxe.core.bukkit.command;

import br.com.muxe.core.bukkit.command.framework.annotation.Arg;
import br.com.muxe.core.bukkit.command.framework.annotation.Command;
import br.com.muxe.core.bukkit.command.framework.annotation.Default;
import br.com.muxe.core.bukkit.command.framework.annotation.Greedy;
import br.com.muxe.core.bukkit.command.framework.annotation.Permission;
import br.com.muxe.core.bukkit.command.framework.annotation.Subcommand;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggest;
import br.com.muxe.core.bukkit.command.framework.annotation.Suggestions;
import br.com.muxe.core.bukkit.command.framework.annotation.Usage;
import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.placement.Positions;
import br.com.muxe.core.bukkit.shop.Shop;
import br.com.muxe.core.bukkit.shop.catalog.ShopProduct;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.core.common.placement.domain.PlacementIds;
import br.com.muxe.core.common.placement.domain.PlacementKind;
import br.com.muxe.core.common.placement.domain.Position;
import br.com.muxe.core.common.placement.domain.RegisterPlacement;
import br.com.muxe.core.common.shop.domain.Promotion;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = ShopCommand.NAME, aliases = "shop", description = "Opens the shop, or manages its cash registers and promotion.")
public final class ShopCommand {

    public static final String NAME = "loja";
    public static final String PERMISSION = "muxe.shop.manage";

    private static final String REMOVE = "remover";

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

    @Default
    public void open(Player player) {
        withShop(player, current -> current.service().open(player));
    }

    @Usage
    public void usage(Player player) {
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }
        messages.lines("shop-usage").forEach(player::sendMessage);
    }

    @Subcommand("caixa criar")
    @Permission(PERMISSION)
    public void createRegister(Player player, @Arg("id") String typedId) {
        withShop(player, current -> {
            String id = CommandText.id(typedId);
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
        });
    }

    @Subcommand("caixa mover")
    @Permission(PERMISSION)
    public void moveRegister(Player player, @Arg("id") RegisterPlacement register) {
        withShop(player, current -> reply.then(player, placements.save(register.movedTo(Positions.standing(player.getLocation()))),
            saved -> player.sendMessage(messages.get("shop-register-moved", "id", register.id()))));
    }

    @Subcommand("caixa remover")
    @Permission(PERMISSION)
    public void removeRegister(Player player, @Arg("id") RegisterPlacement register) {
        withShop(player, current -> reply.then(player, placements.remove(PlacementKind.REGISTER, register.id()),
            removed -> player.sendMessage(messages.get("shop-register-removed", "id", register.id()))));
    }

    @Subcommand("caixa listar")
    @Permission(PERMISSION)
    public void listRegisters(Player player) {
        withShop(player, current -> {
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
        });
    }

    @Subcommand("vendedor")
    @Permission(PERMISSION)
    public void seller(Player player, @Arg("id") RegisterPlacement register, @Arg(REMOVE) @Suggest(REMOVE) Optional<String> action) {
        withShop(player, current -> {
            if (action.map(word -> word.toLowerCase(Locale.ROOT)).filter(REMOVE::equals).isPresent()) {
                reply.then(player, placements.save(register.withSeller(Optional.empty())),
                    saved -> player.sendMessage(messages.get("shop-seller-removed", "id", register.id())));
                return;
            }
            reply.then(player, placements.save(register.withSeller(Optional.of(Positions.standing(player.getLocation())))),
                saved -> player.sendMessage(messages.get("shop-seller-moved", "id", register.id())));
        });
    }

    @Subcommand("promocao")
    @Permission(PERMISSION)
    public void showPromotion(Player player) {
        withShop(player, current -> {
            Promotion promotion = current.promotion().current();
            player.sendMessage(messages.get("shop-promotion-header", "product", promotion.productId()));
            if (promotion.lines().isEmpty()) {
                player.sendMessage(messages.get("shop-promotion-no-lines"));
            }
            for (int index = 0; index < promotion.lines().size(); index++) {
                String line = promotion.lines().get(index);
                player.sendMessage(messages.get("shop-promotion-line", "line", Integer.toString(index + 1), "text", line.isEmpty() ? CommandText.EMPTY_LINE : Colors.translate(line)));
            }
        });
    }

    @Subcommand("promocao produto")
    @Permission(PERMISSION)
    public void changeProduct(Player player, @Arg("id") @Suggest(provider = "products") String typedProduct) {
        withShop(player, current -> {
            String product = CommandText.id(typedProduct);
            if (current.service().catalog().featurable(product).isEmpty()) {
                player.sendMessage(messages.get("shop-promotion-product-unknown", "product", product));
                return;
            }
            reply.then(player, current.promotion().change(current.promotion().current().featuring(product)),
                saved -> player.sendMessage(messages.get("shop-promotion-product-changed", "product", product)));
        });
    }

    @Subcommand("promocao adicionar")
    @Permission(PERMISSION)
    public void addPromotionLine(Player player, @Arg("texto") @Greedy String text) {
        withShop(player, current -> reply.then(player, current.promotion().change(current.promotion().current().adding(CommandText.line(text))),
            saved -> player.sendMessage(messages.get("shop-promotion-line-added"))));
    }

    @Subcommand("promocao linha")
    @Permission(PERMISSION)
    public void changePromotionLine(Player player, @Arg("número") int number, @Arg("texto") @Greedy String text) {
        withPromotionLine(player, number, (current, promotion) -> reply.then(player,
            current.promotion().change(promotion.replacing(number, CommandText.line(text))),
            saved -> player.sendMessage(messages.get("shop-promotion-line-changed", "line", Integer.toString(number)))));
    }

    @Subcommand("promocao removerlinha")
    @Permission(PERMISSION)
    public void removePromotionLine(Player player, @Arg("número") int number) {
        withPromotionLine(player, number, (current, promotion) -> reply.then(player,
            current.promotion().change(promotion.without(number)),
            saved -> player.sendMessage(messages.get("shop-promotion-line-removed", "line", Integer.toString(number)))));
    }

    @Suggestions("products")
    public List<String> products(CommandSender sender) {
        return shop.map(current -> current.service().catalog().categories().stream()
                .flatMap(category -> category.products().stream())
                .filter(product -> product.page().isPresent())
                .map(ShopProduct::id)
                .toList())
            .orElse(List.of());
    }

    private void withPromotionLine(Player player, int number, PromotionAction action) {
        withShop(player, current -> {
            Promotion promotion = current.promotion().current();
            if (number < 1 || !promotion.hasLine(number)) {
                player.sendMessage(messages.get("shop-promotion-invalid-line", "count", Integer.toString(promotion.lines().size())));
                return;
            }
            action.run(current, promotion);
        });
    }

    private void withShop(Player player, Consumer<Shop> action) {
        if (shop.isEmpty()) {
            player.sendMessage(messages.get("shop-unavailable"));
            return;
        }
        action.accept(shop.get());
    }

    @FunctionalInterface
    private interface PromotionAction {
        void run(Shop shop, Promotion promotion);
    }
}
