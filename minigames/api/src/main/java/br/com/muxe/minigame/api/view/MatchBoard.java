package br.com.muxe.minigame.api.view;

import br.com.muxe.minigame.api.match.Match;
import br.com.muxe.minigame.api.match.Participant;
import br.com.muxe.minigame.api.match.TeamStatus;
import br.com.muxe.minigame.api.settings.BoardSettings;
import br.com.muxe.minigame.api.timeline.Timeline.Upcoming;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MatchBoard {

    public static final int EIGHT_TEAMS = 8;

    private final BoardSettings settings;

    public MatchBoard(BoardSettings settings) {
        this.settings = settings;
    }

    public String title() {
        return settings.title();
    }

    public List<String> lines(Match match, UUID viewer, Optional<Upcoming> upcoming, Map<String, String> extra) {
        return switch (match.phase()) {
            case WAITING -> fill(settings.waiting(), match, viewer, upcoming, extra);
            case STARTING -> fill(settings.starting(), match, viewer, upcoming, extra);
            case PLAYING, ENDING -> fill(match.teams().size() >= EIGHT_TEAMS ? settings.playingEightTeams() : settings.playing(), match, viewer, upcoming, extra);
        };
    }

    public String teamLine(Match match, TeamColor color, Optional<TeamColor> viewerTeam) {
        TeamStatus status = match.status(color);
        String mark = switch (status.kind()) {
            case RESPAWNS -> settings.online();
            case RESPAWNS_OFFLINE -> settings.offline();
            case LAST_LIFE -> settings.players().replace("{count}", String.valueOf(status.alive()));
            case ELIMINATED -> settings.eliminated();
        };
        return settings.team()
            .replace("{color}", color.color())
            .replace("{letter}", String.valueOf(match.teamLetter(color)))
            .replace("{name}", match.teamName(color))
            .replace("{status}", mark)
            .replace("{you}", viewerTeam.filter(color::equals).isPresent() ? settings.you() : "");
    }

    private List<String> fill(List<String> template, Match match, UUID viewer, Optional<Upcoming> upcoming, Map<String, String> extra) {
        Optional<Participant> player = match.player(viewer);
        Optional<TeamColor> team = player.map(Participant::team);
        List<String> lines = new ArrayList<>();
        for (String line : template) {
            if (line.equals("{teams}")) {
                match.teams().keySet().forEach(color -> lines.add(teamLine(match, color, team)));
                continue;
            }
            if (line.equals("{event}")) {
                lines.add(upcoming
                    .map(next -> settings.event().replace("{event}", next.name()).replace("{time}", next.clock()))
                    .orElse(settings.noEvent()));
                continue;
            }
            String filled = line
                .replace("{code}", match.label())
                .replace("{map}", match.arena().displayName())
                .replace("{players}", String.valueOf(match.players().size()))
                .replace("{max}", String.valueOf(match.mode().maxPlayers()))
                .replace("{seconds}", String.valueOf(match.countdown().remaining().orElse(0)))
                .replace("{mode}", match.mode().displayName())
                .replace("{winstreak}", "0")
                .replace("{kills}", String.valueOf(player.map(Participant::kills).orElse(0)))
                .replace("{final-kills}", String.valueOf(player.map(Participant::finalKills).orElse(0)));
            for (Map.Entry<String, String> placeholder : extra.entrySet()) {
                filled = filled.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
            }
            lines.add(filled);
        }
        return lines;
    }
}
