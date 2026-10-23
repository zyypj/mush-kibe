package br.com.muxe.core.common.messaging.channel;

import java.util.Objects;

public sealed interface PacketTarget {

    static PacketTarget broadcast() {
        return Broadcast.INSTANCE;
    }

    static PacketTarget proxy() {
        return Proxy.INSTANCE;
    }

    static PacketTarget group(String name) {
        return new Group(name);
    }

    static PacketTarget server(String id) {
        return new Server(id);
    }

    record Broadcast() implements PacketTarget {
        private static final Broadcast INSTANCE = new Broadcast();
    }

    record Proxy() implements PacketTarget {
        private static final Proxy INSTANCE = new Proxy();
    }

    record Group(String name) implements PacketTarget {
        public Group {
            Objects.requireNonNull(name, "name");
        }
    }

    record Server(String id) implements PacketTarget {
        public Server {
            Objects.requireNonNull(id, "id");
        }
    }
}
