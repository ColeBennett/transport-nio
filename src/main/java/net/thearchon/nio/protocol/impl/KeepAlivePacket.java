package net.thearchon.nio.protocol.impl;

import io.netty.buffer.ByteBuf;
import net.thearchon.nio.protocol.LengthAwarePacket;

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
