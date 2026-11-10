package br.com.muxe.minigame.api.match;

import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ChatView(Set<UUID> team, Set<UUID> everyone, String prefix) {

    public ChatView {
        team = Set.copyOf(team);
        everyone = Set.copyOf(everyone);
    }

    public Set<UUID> audience(boolean global) {
        return global ? everyone : team;
    }

    public boolean teamOnly() {
        return !team.equals(everyone);
    }

    public static Map<UUID, ChatView> of(Match match, Set<UUID> online) {
        boolean teamsShown = match.phase().showsTeams();
        boolean teamChat = match.phase() == MatchPhase.PLAYING && match.mode().teamSize() > 1;
        Map<UUID, ChatView> views = new HashMap<>();
        for (Participant player : match.players()) {
            TeamColor team = player.team();
            String prefix = teamsShown && team != null ? team.color() + "[" + match.teamLetter(team) + "] " : "";
            Set<UUID> audience = teamChat && team != null ? mates(match, team, online) : online;
            views.put(player.id(), new ChatView(audience, online, prefix));
        }
        return views;
    }

    private static Set<UUID> mates(Match match, TeamColor team, Set<UUID> online) {
        Set<UUID> mates = new HashSet<>();
        for (Participant member : match.members(team)) {
            if (online.contains(member.id())) {
                mates.add(member.id());
            }
        }
        return mates;
    }
}
