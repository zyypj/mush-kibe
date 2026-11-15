package br.com.muxe.core.bukkit.mission.menu;

import br.com.muxe.core.bukkit.credit.CreditFormat;
import br.com.muxe.core.bukkit.mission.config.MissionConfig;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.core.common.mission.domain.Mission;
import br.com.muxe.core.common.mission.domain.MissionBoard;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MissionView {

    private static final long HOURS_PER_DAY = 24;
    private static final long MINUTES_PER_HOUR = 60;

    private MissionView() {
    }

    public static List<String> categoryLore(MissionConfig.Menu menu, long done, long total, Duration remaining) {
        String time = remaining(menu.time(), remaining);
        return menu.categoryLore().stream()
            .map(line -> line.replace("{done}", String.valueOf(done)).replace("{total}", String.valueOf(total)).replace("{time}", time))
            .toList();
    }

    public static int rows(MissionConfig.Menu menu, boolean back) {
        return back ? menu.rowsWithBack() : menu.rows();
    }

    public static String name(MissionConfig.Menu menu, MissionConfig.Texts texts, MissionBoard.Entry entry) {
        String pattern = entry.progress().completed() ? menu.completedName() : menu.mission().name();
        return Colors.translate(pattern.replace("{name}", texts.name()));
    }

    public static List<String> missionLore(MissionConfig.Menu menu, MissionConfig.Texts texts, MissionBoard.Entry entry, String label) {
        Mission mission = entry.mission();
        String progress = menu.progress()
            .replace("{progress}", CreditFormat.format(entry.progress().progress()))
            .replace("{target}", CreditFormat.format(mission.target()));
        List<String> rewards = new ArrayList<>();
        if (mission.xp() > 0) {
            rewards.add(menu.xpReward().replace("{xp}", CreditFormat.format(mission.xp())));
        }
        if (mission.credits() > 0) {
            rewards.add(menu.creditsReward().replace("{credits}", CreditFormat.format(mission.credits())));
        }
        List<String> lore = new ArrayList<>();
        lore.add("&7" + label);
        lore.add(" ");
        texts.description().forEach(line -> lore.add(line.replace("{progress}", progress)));
        if (!rewards.isEmpty()) {
            lore.add(" ");
            lore.add(rewards.size() == 1 ? menu.rewardOne() : menu.rewardMany());
            lore.addAll(rewards);
        }
        lore.add(" ");
        lore.add(entry.progress().completed() ? menu.completed() : menu.inProgress());
        return lore;
    }

    public static String remaining(Map<String, String> texts, Duration remaining) {
        long minutes = Math.max(1, Math.round(remaining.toSeconds() / 60.0));
        long hours = Math.round(minutes / (double) MINUTES_PER_HOUR);
        long days = Math.round(minutes / (double) (MINUTES_PER_HOUR * HOURS_PER_DAY));
        if (days >= 1 && minutes >= MINUTES_PER_HOUR * HOURS_PER_DAY) {
            return unit(texts, "day", "days", days);
        }
        if (hours >= 1 && minutes >= MINUTES_PER_HOUR) {
            return unit(texts, "hour", "hours", hours);
        }
        return unit(texts, "minute", "minutes", minutes);
    }

    private static String unit(Map<String, String> texts, String one, String many, long amount) {
        return amount == 1 ? texts.getOrDefault(one, "1 " + one) : texts.getOrDefault(many, "{n} " + many).replace("{n}", String.valueOf(amount));
    }
}
