package br.com.muxe.lobby.shop.checkout;

import br.com.muxe.core.bukkit.item.ItemBuilder;
import java.time.Duration;
import java.util.Objects;

public record CheckoutSettings(
    String methodTitle,
    String supporterTitle,
    String loadingTitle,
    ItemBuilder pix,
    ItemBuilder card,
    ItemBuilder summary,
    ItemBuilder back,
    ItemBuilder useCode,
    ItemBuilder skipCode,
    ItemBuilder litPane,
    ItemBuilder dimPane,
    long stepTicks,
    long durationTicks,
    Duration codeTimeout,
    String paymentUrl,
    int chatCenter
) {

    public CheckoutSettings {
        Objects.requireNonNull(methodTitle, "methodTitle");
        Objects.requireNonNull(supporterTitle, "supporterTitle");
        Objects.requireNonNull(loadingTitle, "loadingTitle");
        Objects.requireNonNull(pix, "pix");
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(back, "back");
        Objects.requireNonNull(useCode, "useCode");
        Objects.requireNonNull(skipCode, "skipCode");
        Objects.requireNonNull(litPane, "litPane");
        Objects.requireNonNull(dimPane, "dimPane");
        Objects.requireNonNull(codeTimeout, "codeTimeout");
        Objects.requireNonNull(paymentUrl, "paymentUrl");
        LoadingRing.steps(durationTicks, stepTicks);
    }
}
