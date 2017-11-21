package me.bennettca.nio.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.bennettca.nio.BufferedPacket;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.PacketRegistry;

public class RequestPacket extends Packet {

    public static final short PACKET_ID = -5;

    private long uniqueId;
    private Packet packet;
    private PacketRegistry registry;

    public RequestPacket(PacketRegistry registry) {
        super(PACKET_ID);

        this.registry = registry;
    }

    public RequestPacket(long uniqueId, Packet packet) {
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
        StringBuilder buf = new StringBuilder("RequestPacket(");
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
