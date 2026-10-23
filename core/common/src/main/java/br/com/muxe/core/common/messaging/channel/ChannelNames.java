package br.com.muxe.core.common.messaging.channel;

import br.com.muxe.core.common.server.ServerIdentity;
import java.util.ArrayList;
import java.util.List;

public final class ChannelNames {

    public static final String DEFAULT_PREFIX = "muxe:packets";

    private final String prefix;

    public ChannelNames() {
        this(DEFAULT_PREFIX);
    }

    public ChannelNames(String prefix) {
        this.prefix = prefix;
    }

    public String of(PacketTarget target) {
        return switch (target) {
            case PacketTarget.Broadcast broadcast -> prefix + ":broadcast";
            case PacketTarget.Proxy proxy -> prefix + ":proxy";
            case PacketTarget.Group group -> prefix + ":group:" + group.name();
            case PacketTarget.Server server -> prefix + ":server:" + server.id();
        };
    }

    public List<String> subscriptionsFor(ServerIdentity identity) {
        List<String> channels = new ArrayList<>();
        channels.add(of(PacketTarget.broadcast()));
        channels.add(of(PacketTarget.group(identity.group())));
        channels.add(of(PacketTarget.server(identity.id())));
        if (identity.isProxy()) {
            channels.add(of(PacketTarget.proxy()));
        }
        return List.copyOf(channels);
    }
}
