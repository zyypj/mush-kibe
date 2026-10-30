package br.com.muxe.minigame.engine.setup;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.List;

public record TeamRule(List<TeamColor> fixed, int count) {

    public TeamRule {
        fixed = List.copyOf(fixed);
        if (!fixed.isEmpty() && fixed.size() != count) {
            throw new IllegalArgumentException("Fixed teams " + fixed + " do not match the count " + count);
        }
        if (count < 1 || count > TeamColor.values().length) {
            throw new IllegalArgumentException("Invalid team count " + count);
        }
    }

    public static TeamRule fixed(TeamColor... teams) {
        return new TeamRule(List.of(teams), teams.length);
    }

    public static TeamRule chosen(int count) {
        return new TeamRule(List.of(), count);
    }

    public boolean isChosen() {
        return fixed.isEmpty();
    }
}
