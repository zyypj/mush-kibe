package br.com.muxe.core.bukkit.command.framework.argument;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.muxe.core.bukkit.command.framework.FakeSender;
import br.com.muxe.core.bukkit.command.framework.message.CommandMessages;
import java.lang.reflect.Proxy;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class ArgumentTypesTest {

    private static final CommandSender SENDER = FakeSender.named("CONSOLE").sender();

    @Test
    void primitivesShareTheirBoxedTypes() {
        ArgumentTypes types = ArgumentTypes.basic();

        assertThat(types.find(int.class)).containsSame(types.find(Integer.class).orElseThrow());
        assertThat(types.find(boolean.class)).isPresent();
        assertThat(types.find(StringBuilder.class)).isEmpty();
    }

    @Test
    void numbersRejectWhatIsNotANumber() {
        assertThat(NumberArgument.integers().parse(SENDER, "42")).isEqualTo(ArgumentResult.of(42));
        assertThat(NumberArgument.longs().parse(SENDER, "-7")).isEqualTo(ArgumentResult.of(-7L));
        assertThat(NumberArgument.doubles().parse(SENDER, "2,5")).isEqualTo(ArgumentResult.of(2.5));
        assertThat(NumberArgument.integers().parse(SENDER, "dois")).isEqualTo(ArgumentResult.rejected(CommandMessages.INVALID_NUMBER, "input", "dois"));
        assertThat(NumberArgument.doubles().parse(SENDER, "NaN")).isEqualTo(ArgumentResult.rejected(CommandMessages.INVALID_NUMBER, "input", "NaN"));
    }

    @Test
    void booleansAcceptPortugueseAndEnglishWords() {
        BooleanArgument booleans = new BooleanArgument();

        assertThat(booleans.parse(SENDER, "SIM")).isEqualTo(ArgumentResult.of(true));
        assertThat(booleans.parse(SENDER, "nao")).isEqualTo(ArgumentResult.of(false));
        assertThat(booleans.parse(SENDER, "off")).isEqualTo(ArgumentResult.of(false));
        assertThat(booleans.parse(SENDER, "talvez")).isInstanceOf(ArgumentResult.Rejected.class);
    }

    @Test
    void onlinePlayersAreFoundByTheirExactName() {
        Player ana = FakeSender.named("Ana").player();
        Server server = server(ana);
        PlayerArgument players = new PlayerArgument(server);

        assertThat(players.parse(SENDER, "Ana")).isEqualTo(ArgumentResult.of(ana));
        assertThat(players.parse(SENDER, "Bia")).isEqualTo(ArgumentResult.rejected(CommandMessages.PLAYER_OFFLINE, "input", "Bia"));
        assertThat(players.suggest(SENDER)).containsExactly("Ana");
        assertThat(ArgumentTypes.standard(server).find(Player.class)).isPresent();
    }

    private static Server server(Player online) {
        return (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[] {Server.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getPlayerExact" -> online.getName().equals(args[0]) ? online : null;
            case "getOnlinePlayers" -> List.of(online);
            default -> null;
        });
    }
}
