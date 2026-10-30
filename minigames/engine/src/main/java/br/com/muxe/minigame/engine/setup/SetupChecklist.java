package br.com.muxe.minigame.engine.setup;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.List;

public record SetupChecklist(TeamRule teamRule, int teamCount, List<Item> items) {

    public SetupChecklist {
        items = List.copyOf(items);
    }

    public static SetupChecklist of(SetupSchema schema, ArenaDraft draft) {
        List<Item> items = new ArrayList<>();
        for (SetupStep step : schema.steps(draft.mode())) {
            if (step.perTeam()) {
                for (TeamColor team : draft.teams()) {
                    items.add(new Item(step, team, draft.count(step, team)));
                }
            } else {
                items.add(new Item(step, null, draft.count(step, null)));
            }
        }
        return new SetupChecklist(schema.teams(draft.mode()), draft.teams().size(), items);
    }

    public boolean teamsDone() {
        return teamCount == teamRule.count();
    }

    public List<Item> missing() {
        return items.stream().filter(item -> !item.done()).toList();
    }

    public boolean isReady() {
        return teamsDone() && missing().isEmpty();
    }

    public int missingCount() {
        return missing().size() + (teamsDone() ? 0 : 1);
    }

    public record Item(SetupStep step, TeamColor team, int count) {

        public boolean done() {
            return count >= step.minimum();
        }

        public boolean full() {
            return count >= step.maximum();
        }
    }
}
