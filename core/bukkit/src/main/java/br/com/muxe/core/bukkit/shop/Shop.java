package br.com.muxe.core.bukkit.shop;

import br.com.muxe.core.bukkit.placement.PlacementSync;
import br.com.muxe.core.bukkit.shop.promotion.ShopPromotion;
import br.com.muxe.core.bukkit.shop.register.ShopRegisters;
import br.com.muxe.core.common.placement.domain.Placements;
import br.com.muxe.core.common.shop.domain.Promotion;

public final class Shop {

    private final ShopService service;
    private final ShopPromotion promotion;
    private final ShopRegisters registers;

    public Shop(ShopService service, ShopPromotion promotion, ShopRegisters registers) {
        this.service = service;
        this.promotion = promotion;
        this.registers = registers;
    }

    public void start(PlacementSync placements) {
        promotion.onChange(this::showPromotion);
        placements.onChange(this::showRegisters);
        promotion.start();
    }

    public ShopService service() {
        return service;
    }

    public ShopPromotion promotion() {
        return promotion;
    }

    public void stop() {
        promotion.stop();
        registers.stop();
        service.stop();
    }

    private void showPromotion(Promotion current) {
        registers.label(current.lines());
    }

    private void showRegisters(Placements placements) {
        registers.show(placements.registers());
    }
}
