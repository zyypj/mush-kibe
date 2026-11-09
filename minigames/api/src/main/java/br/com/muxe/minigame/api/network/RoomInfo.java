package br.com.muxe.minigame.api.network;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;

public record RoomInfo(String code, String mode, String arena, boolean open, int players, int maxPlayers) {

    public boolean hasRoom() {
        return hasRoom(1);
    }

    public boolean hasRoom(int seats) {
        return open && players + seats <= maxPlayers;
    }

    public static void write(PacketWriter out, RoomInfo room) {
        out.writeString(room.code())
            .writeString(room.mode())
            .writeString(room.arena())
            .writeBoolean(room.open())
            .writeVarInt(room.players())
            .writeVarInt(room.maxPlayers());
    }

    public static RoomInfo read(PacketReader in) {
        return new RoomInfo(in.readString(), in.readString(), in.readString(), in.readBoolean(), in.readVarInt(), in.readVarInt());
    }
}
