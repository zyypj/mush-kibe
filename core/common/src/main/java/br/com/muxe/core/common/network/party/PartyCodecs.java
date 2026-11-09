package br.com.muxe.core.common.network.party;

import br.com.muxe.core.common.messaging.codec.PacketReader;
import br.com.muxe.core.common.messaging.codec.PacketWriter;
import br.com.muxe.core.common.party.domain.PartyNotice;
import br.com.muxe.core.common.party.domain.PartySnapshot;

final class PartyCodecs {

    private PartyCodecs() {
    }

    static void writeSnapshot(PacketWriter out, PartySnapshot party) {
        out.writeUuid(party.id()).writeUuid(party.leader()).writeBoolean(party.open())
            .writeList(party.members(), (writer, member) -> writer.writeUuid(member.id()).writeString(member.name()).writeBoolean(member.online()));
    }

    static PartySnapshot readSnapshot(PacketReader in) {
        return new PartySnapshot(in.readUuid(), in.readUuid(), in.readBoolean(),
            in.readList(reader -> new PartySnapshot.Member(reader.readUuid(), reader.readString(), reader.readBoolean())));
    }

    static void writeNotice(PacketWriter out, PartyNotice notice) {
        out.writeList(notice.recipients(), PacketWriter::writeUuid)
            .writeString(notice.key())
            .writeList(notice.placeholders(), PacketWriter::writeString)
            .writeString(notice.click())
            .writeString(notice.raw());
    }

    static PartyNotice readNotice(PacketReader in) {
        return new PartyNotice(in.readList(PacketReader::readUuid), in.readString(), in.readList(PacketReader::readString), in.readString(), in.readString());
    }
}
