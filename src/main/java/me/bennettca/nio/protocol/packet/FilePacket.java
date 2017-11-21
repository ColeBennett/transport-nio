package me.bennettca.nio.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.bennettca.nio.BufferedPacket;
import me.bennettca.nio.protocol.stream.ChunkedFilePacket;

import java.io.File;

public class FilePacket extends ChunkedFilePacket {

    public static final short PACKET_ID = 1;

    private BufferedPacket header;

    public FilePacket() {
        super();
    }

    public FilePacket(File file, BufferedPacket header) {
        super(file);

        this.header = header;
    }

    @Override
    public void read(ByteBuf buf) {
        super.read(buf);
        header = new BufferedPacket(buf);
    }

    @Override
    public void write(ByteBuf buf) {
        super.write(buf);
        header.write(buf);
    }

    @Override
    public int length() {
        return super.length() + header.length();
    }

    @Override
    public short getId() {
        return PACKET_ID;
    }

    public BufferedPacket getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "FilePacket(header: " + header + ", " + super.toString() + ")";
    }
}
