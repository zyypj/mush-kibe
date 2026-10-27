package br.com.muxe.core.bukkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.common.placement.domain.ParkourPoint;
import br.com.muxe.core.common.placement.domain.Position;
import java.lang.reflect.Proxy;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class ParkourCommandTest {

    @Test
    void pointsAreNamedInPortuguese() {
        assertThat(ParkourCommand.point("inicio", "")).contains(ParkourPoint.start());
        assertThat(ParkourCommand.point("início", "")).contains(ParkourPoint.start());
        assertThat(ParkourCommand.point("fim", "")).contains(ParkourPoint.end());
        assertThat(ParkourCommand.point("checkpoint", "3")).contains(ParkourPoint.checkpoint(3));
        assertThat(ParkourCommand.point("2", "")).contains(ParkourPoint.checkpoint(2));
        assertThat(ParkourCommand.point("checkpoint", "0")).isEmpty();
        assertThat(ParkourCommand.point("meio", "")).isEmpty();
    }

    @Test
    void aPointSitsInTheMiddleOfTheBlockWhereTheStaffStandsFacingTheirWay() {
        World lobby = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class},
            (proxy, method, args) -> method.getName().equals("getName") ? "lobby" : null);

        Position position = ParkourCommand.standingOn(new Location(lobby, -36.59, 86.0, 32.81, -165.2f, 16.5f));

        assertThat(position).isEqualTo(new Position("lobby", -36.5, 86, 32.5, -165.2f, 0f));
    }
}
