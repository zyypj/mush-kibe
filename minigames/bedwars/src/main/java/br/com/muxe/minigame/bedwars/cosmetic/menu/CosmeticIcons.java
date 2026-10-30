package br.com.muxe.minigame.bedwars.cosmetic.menu;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.Availability;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.BedCosmetic;
import br.com.muxe.minigame.bedwars.cosmetic.catalog.CosmeticCatalog;
import br.com.muxe.minigame.bedwars.cosmetic.menu.CosmeticMenuSettings.LoreTexts;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticState;
import br.com.muxe.minigame.bedwars.cosmetic.profile.CosmeticWallet;
import java.util.ArrayList;
import java.util.List;

public final class CosmeticIcons {

    public static final String BLANK = " ";

    private final CosmeticMenuSettings settings;
    private final CosmeticCatalog catalog;

    public CosmeticIcons(CosmeticMenuSettings settings, CosmeticCatalog catalog) {
        this.settings = settings;
        this.catalog = catalog;
    }

    public ItemBuilder cosmetic(BedCosmetic cosmetic, CosmeticWallet wallet) {
        LoreTexts texts = settings.lore();
        CosmeticState state = wallet.state(cosmetic);
        boolean owned = state == CosmeticState.SELECTED || state == CosmeticState.OWNED;
        List<String> lore = new ArrayList<>();
        lore.add(stars(cosmetic.stars()));
        lore.add(BLANK);
        if (!cosmetic.description().isEmpty()) {
            lore.addAll(cosmetic.description());
            lore.add(BLANK);
        }
        if (cosmetic.availability() == Availability.ULTRA) {
            lore.add(texts.ultraTag());
            lore.add(BLANK);
        } else if (cosmetic.tag().isPresent()) {
            lore.add(cosmetic.tag().get());
            lore.add(BLANK);
        }
        long price = catalog.price(cosmetic);
        List<String> block = switch (state) {
            case SELECTED -> texts.selectedWithPreview();
            case OWNED -> texts.select();
            case BUY -> wallet.credits() >= price ? texts.buyAffordable() : texts.buy();
            case UNAVAILABLE -> texts.unavailable();
        };
        String missing = CreditFormat.format(Math.max(0, price - wallet.credits()));
        block.forEach(line -> lore.add(line.replace("{price}", CreditFormat.format(price)).replace("{missing}", missing)));
        String name = (owned ? texts.ownedName() : texts.lockedName()).replace("{name}", cosmetic.name());
        return cosmetic.icon().name(name).lore(lore).glow(state == CosmeticState.SELECTED);
    }

    public ItemBuilder none(CosmeticWallet wallet) {
        boolean chosen = CosmeticWallet.NONE.equals(wallet.selected());
        List<String> lore = new ArrayList<>(settings.none().lore());
        lore.add(BLANK);
        lore.addAll(chosen ? settings.lore().selected() : settings.lore().selectOnly());
        return settings.none().lore(lore).glow(chosen);
    }

    public ItemBuilder random(CosmeticWallet wallet) {
        boolean chosen = CosmeticWallet.RANDOM.equals(wallet.selected());
        List<String> lore = new ArrayList<>(settings.random().lore());
        lore.add(BLANK);
        lore.addAll(chosen ? settings.lore().selected() : settings.lore().selectOnly());
        return settings.random().lore(lore).glow(chosen);
    }

    public ItemBuilder selected(CosmeticWallet wallet) {
        List<String> lore = switch (wallet.selected()) {
            case CosmeticWallet.RANDOM -> settings.random().lore();
            case CosmeticWallet.NONE -> List.of();
            default -> catalog.find(wallet.selected()).map(BedCosmetic::description).orElse(List.of());
        };
        return settings.selected().item().replacing("name", selectedName(wallet)).lore(lore);
    }

    public ItemBuilder category(CosmeticWallet wallet) {
        long owned = wallet.ownedCount(catalog);
        int total = catalog.all().size();
        long percent = total == 0 ? 0 : owned * 100 / total;
        return settings.bedDestruction().item().replacing(
            "owned", Long.toString(owned),
            "total", Integer.toString(total),
            "percent", Long.toString(percent),
            "selected", selectedName(wallet)
        );
    }

    public String selectedName(CosmeticWallet wallet) {
        return switch (wallet.selected()) {
            case CosmeticWallet.RANDOM -> plain(settings.random().name());
            case CosmeticWallet.NONE -> plain(settings.none().name());
            default -> catalog.find(wallet.selected()).map(BedCosmetic::name).orElse(plain(settings.random().name()));
        };
    }

    String stars(int count) {
        LoreTexts texts = settings.lore();
        String lit = texts.starsOn() + texts.star().repeat(count);
        int unlit = BedCosmetic.MAX_STARS - count;
        return unlit == 0 ? lit : lit + texts.starsOff() + texts.star().repeat(unlit);
    }

    private static String plain(String colored) {
        return colored == null ? "" : colored.replaceAll("(?i)[§&][0-9a-fk-or]", "");
    }
}
