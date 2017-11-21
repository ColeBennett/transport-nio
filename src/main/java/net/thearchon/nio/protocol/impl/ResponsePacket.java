package net.thearchon.nio.protocol.impl;

import io.netty.buffer.ByteBuf;
import net.thearchon.nio.BufferedPacket;
import net.thearchon.nio.protocol.Packet;
import net.thearchon.nio.protocol.PacketRegistry;

public class ResponsePacket extends Packet {

    public static final short PACKET_ID = -4;

    private long uniqueId;
    private Packet packet;
    private PacketRegistry registry;

    public ResponsePacket(PacketRegistry registry) {
        super(PACKET_ID);

        this.registry = registry;
    }

    public ResponsePacket(long uniqueId, Packet packet) {
        super(PACKET_ID);

        this.uniqueId = uniqueId;
        this.packet = packet;
    }

    @Override
    public void read(ByteBuf buf) {
        uniqueId = buf.readLong();
        short id = buf.readShort();
        if (id == BufferedPacket.PACKET_ID) {
            packet = new BufferedPacket(buf);
        } else {
            packet = registry.newInstance(id);
            packet.read(buf);
        }
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeLong(uniqueId);
        buf.writeShort(packet.getId());
        packet.write(buf);
    }

    public long getUniqueId() {
        return uniqueId;
    }

    public Packet getPacket() {
        return packet;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ResponsePacket(");
        if (packet != null) {
            buf.append("unique id: ");
            buf.append(uniqueId);
            buf.append(", ");
            buf.append("packet: ");
            buf.append(packet.getClass().getSimpleName());
        } else {
            buf.append("empty");
        }
        buf.append(')');
        return buf.toString();
    }
}
