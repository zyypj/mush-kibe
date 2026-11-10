package br.com.muxe.core.proxy.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConnectRepliesTest {

    @Test
    void everyFailureIsAnsweredWithOurOwnMessage() {
        assertThat(ConnectReplies.messageKey(Optional.of(Status.ALREADY_CONNECTED), false)).contains(ConnectReplies.ALREADY_CONNECTED);
        assertThat(ConnectReplies.messageKey(Optional.of(Status.CONNECTION_IN_PROGRESS), false)).contains(ConnectReplies.ALREADY_CONNECTING);
        assertThat(ConnectReplies.messageKey(Optional.of(Status.SERVER_DISCONNECTED), false)).contains(ConnectReplies.CONNECT_FAILED);
        assertThat(ConnectReplies.messageKey(Optional.empty(), false)).contains(ConnectReplies.CONNECT_FAILED);
    }

    @Test
    void aSuccessOrADeniedConnectionSaysNothingMore() {
        assertThat(ConnectReplies.messageKey(Optional.of(Status.SUCCESS), false)).isEmpty();
        assertThat(ConnectReplies.messageKey(Optional.of(Status.CONNECTION_CANCELLED), false)).isEmpty();
    }

    @Test
    void aPartyMemberAlreadyOnTheServerIsNotTold() {
        assertThat(ConnectReplies.messageKey(Optional.of(Status.ALREADY_CONNECTED), true)).isEmpty();
        assertThat(ConnectReplies.messageKey(Optional.of(Status.SERVER_DISCONNECTED), true)).contains(ConnectReplies.CONNECT_FAILED);
    }
}
