package br.com.muxe.lobby.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.muxe.core.bukkit.display.DisplayLabel;
import br.com.muxe.core.bukkit.display.DisplayStand;
import br.com.muxe.core.bukkit.text.MessageCatalog;
import br.com.muxe.lobby.shop.catalog.ShopCatalog;
import br.com.muxe.lobby.shop.catalog.ShopCategory;
import br.com.muxe.lobby.shop.catalog.ShopProduct;
import br.com.muxe.lobby.shop.checkout.CheckoutSettings;
import br.com.muxe.lobby.shop.register.RegisterSettings;
import java.time.Duration;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ShopSettingsTest {

    private final ShopCatalog catalog = ShopFixtures.SETTINGS.catalog();
    private final CheckoutSettings checkout = ShopFixtures.SETTINGS.checkout();
    private final RegisterSettings register = ShopFixtures.SETTINGS.register();

    @Test
    void theTabsFollowTheReferenceMenus() {
        assertThat(catalog.categories()).extracting(ShopCategory::id).containsExactly("ranks", "ultra-plus", "credits", "medals", "orders");
        assertThat(catalog.categories()).extracting(ShopCategory::tabSlot).containsExactly(1, 2, 3, 4, 7);
        assertThat(catalog.categories()).extracting(ShopCategory::name).containsExactly("Ranks", "Ultra+", "Créditos", "Medalhas", "Meus pedidos");
        assertThat(catalog.defaultCategory().id()).isEqualTo("ranks");
        assertThat(catalog.title("Ranks")).isEqualTo("Loja: Ranks");
    }

    @Test
    void theRanksAreSoldInTheThirdRow() {
        ShopCategory ranks = catalog.category("ranks").orElseThrow();

        assertThat(ranks.products()).extracting(ShopProduct::slot).containsExactly(28, 30, 32, 34);
        assertThat(ranks.products()).extracting(ShopProduct::price).containsExactly("R$ 39,99", "R$ 54,99", "R$ 64,99", "");
        assertThat(ranks.products()).extracting(ShopProduct::purchasable).containsExactly(true, true, true, false);
        assertThat(ranks.extras()).containsOnlyKeys(37, 39, 41, 43);
        assertThat(catalog.product("vip").orElseThrow().icon().name()).isEqualTo("§aVIP");
    }

    @Test
    void ultraPlusNeedsUltraSoItCannotBeBoughtYet() {
        ShopCategory ultraPlus = catalog.category("ultra-plus").orElseThrow();

        assertThat(ultraPlus.products()).allMatch(product -> !product.purchasable());
        assertThat(ultraPlus.products()).extracting(product -> product.icon().amount()).containsExactly(1, 3, 6, 12);
        assertThat(ultraPlus.extras()).containsOnlyKeys(49);
    }

    @Test
    void creditPacksAreHeadsWithTheirOwnTextures() {
        ShopProduct pack = catalog.product("credits-900").orElseThrow();

        assertThat(pack.icon().material()).isEqualTo(Material.SKULL_ITEM);
        assertThat(pack.icon().skullTexture()).startsWith("eyJ0ZXh0dXJlcyI6");
        assertThat(pack.icon().name()).isEqualTo("§6900 Créditos (+100 Bônus)");
    }

    @Test
    void enderloreIsTheFeaturedProductWithItsOwnPage() {
        ShopProduct featured = catalog.featured();

        assertThat(featured.id()).isEqualTo("enderlore");
        assertThat(featured.page()).isPresent();
        assertThat(featured.page().orElseThrow().name()).isEqualTo("Enderlore");
        assertThat(featured.page().orElseThrow().icon().lore()).endsWith("§eClique para ver a lista", "§ecompleta de vantagens!");
    }

    @Test
    void myOrdersShowsNoOrders() {
        ShopCategory orders = catalog.category("orders").orElseThrow();

        assertThat(orders.products()).isEmpty();
        assertThat(orders.extras().get(31).name()).isEqualTo("§cNenhum pedido encontrado!");
    }

    @Test
    void theCheckoutUsesTheReferenceTitlesAndTiming() {
        assertThat(checkout.methodTitle()).isEqualTo("Compra: Método de pagamento");
        assertThat(checkout.supporterTitle()).isEqualTo("Compra: Código de apoiador");
        assertThat(checkout.loadingTitle()).isEqualTo("Compra: Carregando...");
        assertThat(checkout.stepTicks()).isEqualTo(3);
        assertThat(checkout.durationTicks()).isEqualTo(60);
        assertThat(checkout.codeTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(checkout.paymentUrl()).isEqualTo("https://muxe.com.br/loja");
        assertThat(checkout.card().material()).isEqualTo(Material.BANNER);
        assertThat(checkout.card().data()).isEqualTo((short) 5);
    }

    @Test
    void theRegisterIsMadeOfEightHeadsAndTwoSpinningDyes() {
        List<DisplayStand> heads = register.parts().stream().filter(part -> part.helmet().isPresent()).toList();
        List<DisplayStand> dyes = register.parts().stream().filter(part -> part.hand().isPresent()).toList();

        assertThat(heads).hasSize(8).allMatch(DisplayStand::small);
        assertThat(heads).filteredOn(part -> part.headPose().x() == -45).hasSize(2);
        assertThat(dyes).hasSize(2).allMatch(part -> part.arms() && part.motion().moves());
        assertThat(Math.abs(dyes.get(0).yaw() - dyes.get(1).yaw())).isCloseTo(180f, within(0.01f));
        assertThat(dyes).allSatisfy(dye -> assertThat(2 * dye.motion().bobHeight()).isCloseTo(0.5, within(1e-9)));
        assertThat(register.seller()).isTrue();
        assertThat(register.sellerSkin()).isPresent();
    }

    @Test
    void theHologramListsTheOfferFromTopToBottom() {
        assertThat(register.labels()).extracting(DisplayLabel::text).containsExactly(
            "&b&lLOJA", "&5&lRANK ENDERLORE", "&b&l13% DE DESCONTO!", "&2&mR$ 74,99&r &aR$ 64,99", "&eClique para comprar!");
        assertThat(register.labels()).extracting(DisplayLabel::height).isSortedAccordingTo((first, second) -> Double.compare(second, first));
    }

    @Test
    void thePaymentMessageMatchesTheReference() {
        MessageCatalog messages = MessageCatalog.from(ShopFixtures.CONFIG.getConfigurationSection("messages"));

        List<String> payment = messages.lines("shop-payment");

        assertThat(payment).hasSize(9);
        assertThat(payment.get(0)).startsWith("§a§m ").isEqualTo(payment.get(8));
        assertThat(payment.get(1)).isEqualTo("<center>§b§lPAGAMENTO");
        assertThat(payment.get(3)).isEqualTo("<center>§eEscaneie o §aQR Code §eem sua tela para");
        assertThat(payment.get(4)).isEqualTo("<center>§efinalizar o §6pagamento§e!");
        assertThat(payment.get(6)).isEqualTo("<link>§cClique aqui para fazer o pagamento via o site.");
    }
}
