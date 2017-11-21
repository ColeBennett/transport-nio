package me.bennettca.nio.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.bennettca.nio.protocol.LengthAwarePacket;

public class ByteBufPacket extends LengthAwarePacket {

    public static final short PACKET_ID = -1;

    private ByteBuf buffer;

    public ByteBufPacket() {
        super(PACKET_ID);
    }

    public ByteBufPacket(ByteBuf buffer) {
        super(PACKET_ID);

        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        this.buffer = buffer;
    }

    @Override
    public void read(ByteBuf buf) {
        buffer = buf.copy();
        buf.skipBytes(buf.readableBytes());
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeBytes(buffer);
        buffer.release();
        buffer = null;
    }

    @Override
    public int length() {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        return buffer.readableBytes();
    }

    public ByteBuf buffer() {
        return buffer;
    }
}
