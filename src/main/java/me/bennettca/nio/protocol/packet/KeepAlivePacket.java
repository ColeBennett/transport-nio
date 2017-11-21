package me.bennettca.nio.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.bennettca.nio.protocol.LengthAwarePacket;

public final class KeepAlivePacket extends LengthAwarePacket {

    public static final KeepAlivePacket INSTANCE = new KeepAlivePacket();
    public static final short PACKET_ID = -6;

    private KeepAlivePacket() {
        super(PACKET_ID);
    }

    @Override
    public void read(ByteBuf buf) {

    }

    @Override
    public void write(ByteBuf buf) {

    }

    @Override
    public int length() {
        return 0;
    }
}
