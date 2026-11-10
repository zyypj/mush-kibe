package br.com.muxe.minigame.api.view;

import br.com.muxe.core.bukkit.permission.display.NameTag;
import br.com.muxe.core.bukkit.text.Colors;
import br.com.muxe.minigame.engine.team.TeamColor;

public final class NameTagStyle {

    private NameTagStyle() {
    }

    public static NameTag of(String prefix, TeamColor team) {
        return of(prefix, team, team.letter());
    }

    public static NameTag of(String prefix, TeamColor team, char letter) {
        String name = String.format("%s%02d%s", prefix, team.ordinal(), team.id());
        return new NameTag(name.length() > NameTag.MAX_TEAM ? name.substring(0, NameTag.MAX_TEAM) : name,
            Colors.translate(team.color() + "&l" + letter + team.color() + " "));
    }
}
