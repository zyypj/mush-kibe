package br.com.muxe.core.bukkit.stats;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.item.ItemBuilder;
import br.com.muxe.core.bukkit.menu.button.ConfiguredButton;
import br.com.muxe.core.common.stats.domain.BedWarsLevels;
import br.com.muxe.core.common.stats.domain.BedWarsTotals;
import br.com.muxe.core.common.stats.domain.ModeTotals;
import java.util.ArrayList;
import java.util.List;

public final class StatsPages {

    private StatsPages() {
    }

    public static List<Shown> hub(StatsSettings.Hub hub, boolean withBack) {
        List<Shown> shown = new ArrayList<>();
        for (StatsSettings.Game game : hub.games()) {
            Click click = switch (game.opens()) {
                case NOTHING -> Click.NOTHING;
                case SOON -> Click.SOON;
                case BEDWARS -> Click.BEDWARS;
            };
            shown.add(new Shown(game.button().slot(), game.button().item(), click));
        }
        if (withBack) {
            shown.add(new Shown(hub.back().slot(), hub.back().item(), Click.BACK));
        }
        return shown;
    }

    public static List<Shown> bedwars(StatsSettings.BedWars page, BedWarsTotals totals, boolean withBack, String parentTitle) {
        List<Shown> shown = new ArrayList<>();
        ModeTotals general = totals.general();
        String level = page.badges().of(BedWarsLevels.progress(Math.max(0, general.xp())).level());
        ConfiguredButton head = page.general();
        shown.add(new Shown(head.slot(), withLines(head.item(), head.item().lore().stream().map(line -> line(line, general).replace("{level}", level)).toList()),
            Click.NOTHING));
        for (ConfiguredButton mode : page.modes()) {
            ModeTotals counters = totals.of(mode.id());
            shown.add(new Shown(mode.slot(), withLines(mode.item(), page.lore().stream().map(line -> line(line, counters)).toList()), Click.NOTHING));
        }
        if (withBack) {
            ItemBuilder back = page.back().item();
            shown.add(new Shown(page.back().slot(), withLines(back, back.lore().stream().map(line -> line.replace("{parent}", parentTitle)).toList()), Click.BACK));
        }
        return shown;
    }

    public static String line(String line, ModeTotals totals) {
        return line
            .replace("{games}", CreditFormat.format(totals.games()))
            .replace("{beds-broken}", CreditFormat.format(totals.bedsBroken()))
            .replace("{beds-lost}", CreditFormat.format(totals.bedsLost()))
            .replace("{kills}", CreditFormat.format(totals.kills()))
            .replace("{deaths}", CreditFormat.format(totals.deaths()))
            .replace("{assists}", CreditFormat.format(totals.assists()))
            .replace("{final-kills}", CreditFormat.format(totals.finalKills()))
            .replace("{final-deaths}", CreditFormat.format(totals.finalDeaths()))
            .replace("{final-assists}", CreditFormat.format(totals.finalAssists()))
            .replace("{wins}", CreditFormat.format(totals.wins()))
            .replace("{losses}", CreditFormat.format(totals.losses()))
            .replace("{best-winstreak}", CreditFormat.format(totals.bestWinstreak()))
            .replace("{winstreak}", CreditFormat.format(totals.winstreak()))
            .replace("{xp}", CreditFormat.format(totals.xp()));
    }

    private static ItemBuilder withLines(ItemBuilder item, List<String> lore) {
        return item.lore(lore);
    }

    public enum Click {
        NOTHING,
        SOON,
        BEDWARS,
        BACK
    }

    public record Shown(int slot, ItemBuilder icon, Click click) {
    }
}
