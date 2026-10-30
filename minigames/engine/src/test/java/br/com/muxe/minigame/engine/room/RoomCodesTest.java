package br.com.muxe.minigame.engine.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoomCodesTest {

    @Test
    void codesLookLikeTheReferenceAndAreNeverRepeated() {
        RoomCodes codes = new RoomCodes(new Random(7));
        Set<String> taken = new HashSet<>();

        for (int index = 0; index < 500; index++) {
            String code = codes.next(taken);
            assertThat(code).matches("[0-9][0-9a-z]{5}");
            assertThat(taken.add(code)).isTrue();
        }
    }
}
