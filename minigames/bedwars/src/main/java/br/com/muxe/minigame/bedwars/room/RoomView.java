package br.com.muxe.minigame.bedwars.room;

import br.com.muxe.minigame.bedwars.game.TeamStatus;
import br.com.muxe.minigame.bedwars.game.Timeline;
import br.com.muxe.minigame.bedwars.settings.BedWarsSettings.ScoreboardSettings;
import br.com.muxe.minigame.engine.team.TeamColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RoomView {

    public static final int EIGHT_TEAMS = 8;

    private final ScoreboardSettings settings;
    private final Timeline timeline;

    public RoomView(ScoreboardSettings settings, Timeline timeline) {
        this.settings = settings;
        this.timeline = timeline;
    }

    public String title() {
        return settings.title();
    }

    public List<String> lines(Room room, UUID viewer, int winstreak) {
        return switch (room.phase()) {
            case WAITING -> fill(settings.waiting(), room, viewer, winstreak);
            case STARTING -> fill(settings.starting(), room, viewer, winstreak);
            case PLAYING, ENDING -> fill(room.teams().size() >= EIGHT_TEAMS ? settings.playingEightTeams() : settings.playing(), room, viewer, winstreak);
        };
    }

    public String teamLine(Room room, TeamColor color, Optional<TeamColor> viewerTeam) {
        TeamStatus status = room.status(color);
        String mark = switch (status.kind()) {
            case BED_ONLINE -> settings.online();
            case BED_OFFLINE -> settings.offline();
            case NO_BED -> settings.players().replace("{count}", String.valueOf(status.alive()));
            case ELIMINATED -> settings.eliminated();
        };
        return settings.team()
            .replace("{color}", color.color())
            .replace("{letter}", String.valueOf(color.letter()))
            .replace("{name}", color.displayName())
            .replace("{status}", mark)
            .replace("{you}", viewerTeam.filter(color::equals).isPresent() ? settings.you() : "");
    }

    private List<String> fill(List<String> template, Room room, UUID viewer, int winstreak) {
        Optional<RoomPlayer> player = room.player(viewer);
        Optional<TeamColor> team = player.map(RoomPlayer::team);
        List<String> lines = new ArrayList<>();
        for (String line : template) {
            if (line.equals("{teams}")) {
                room.teams().keySet().forEach(color -> lines.add(teamLine(room, color, team)));
                continue;
            }
            if (line.equals("{event}")) {
                lines.add(timeline.next(room.elapsed())
                    .map(next -> settings.event().replace("{event}", next.event().name()).replace("{time}", next.clock()))
                    .orElse(settings.noEvent()));
                continue;
            }
            lines.add(line
                .replace("{code}", room.code())
                .replace("{map}", room.arena().displayName())
                .replace("{players}", String.valueOf(room.players().size()))
                .replace("{max}", String.valueOf(room.mode().maxPlayers()))
                .replace("{seconds}", String.valueOf(room.countdown().remaining().orElse(0)))
                .replace("{mode}", room.mode().displayName())
                .replace("{winstreak}", String.valueOf(winstreak))
                .replace("{kills}", String.valueOf(player.map(RoomPlayer::kills).orElse(0)))
                .replace("{final-kills}", String.valueOf(player.map(RoomPlayer::finalKills).orElse(0)))
                .replace("{beds}", String.valueOf(player.map(RoomPlayer::beds).orElse(0))));
        }
        return lines;
    }
}
