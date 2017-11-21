package net.thearchon.nio.protocol;

import io.netty.handler.codec.UnsupportedMessageTypeException;
import net.thearchon.nio.BufferedPacket;
import net.thearchon.nio.FilePacket;
import net.thearchon.nio.JsonPacket;
import net.thearchon.nio.protocol.impl.ByteBufPacket;
import net.thearchon.nio.protocol.impl.KeepAlivePacket;
import net.thearchon.nio.protocol.impl.RequestPacket;
import net.thearchon.nio.protocol.impl.ResponsePacket;
import net.thearchon.nio.protocol.stream.ChunkedFilePacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketRegistry {

    private final Map<Short, Class<? extends Packet>> packets;

    public PacketRegistry() {
        packets = new ConcurrentHashMap<>(8);

        packets.put(JsonPacket.PACKET_ID, JsonPacket.class); // -7
        packets.put(KeepAlivePacket.PACKET_ID, KeepAlivePacket.class); // -6
        packets.put(RequestPacket.PACKET_ID, RequestPacket.class); // -5
        packets.put(ResponsePacket.PACKET_ID, ResponsePacket.class); // -4
        packets.put(BufferedPacket.PACKET_ID, BufferedPacket.class); // -3
        packets.put(ChunkedFilePacket.PACKET_ID, ChunkedFilePacket.class); // -2
        packets.put(ByteBufPacket.PACKET_ID, ByteBufPacket.class); // -1
        packets.put(FilePacket.PACKET_ID, FilePacket.class); // 1
    }

    public Packet newInstance(short id) {
        Class<? extends Packet> clazz = packets.get(id);
        if (clazz != null) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void validate(Object msg) {
        if (msg == null) {
            throw new NullPointerException("packet");
        }
        if (!(msg instanceof Packet)) {
            throw new UnsupportedMessageTypeException(msg.getClass().getSimpleName());
        }
        short id = ((Packet) msg).getId();
        if (!isRegistered(id)) {
            throw new UnsupportedMessageTypeException("Unknown packet id: " + id);
        }
    }

    public boolean register(short id, Class<? extends Packet> clazz) {
        if (id < 0) {
            throw new IllegalArgumentException("Packet id cannot be negative: " + id);
        }
        if (clazz == null) {
            throw new NullPointerException("packet class");
        }
        return packets.put(id, clazz) != null;
    }

    public boolean unregister(short id) {
        if (id < 0) {
            throw new IllegalArgumentException("Packet id cannot be negative: " + id);
        }
        return packets.remove(id) != null;
    }

    public boolean isRegistered(short id) {
        return packets.containsKey(id);
    }

    public void clear() {
        packets.clear();
    }
}
