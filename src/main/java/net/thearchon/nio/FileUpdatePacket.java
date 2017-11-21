package net.thearchon.nio;

import io.netty.buffer.ByteBuf;
import net.thearchon.nio.protocol.stream.ChunkedFilePacket;

import java.io.File;

public class FileUpdatePacket extends ChunkedFilePacket {

    private boolean restart;

    public FileUpdatePacket() {
        super();
    }

    public FileUpdatePacket(File file, boolean restart) {
        super(file);

        this.restart = restart;
    }

    @Override
    public void read(ByteBuf buf) {
        super.read(buf);
        restart = buf.readBoolean();
    }

    @Override
    public void write(ByteBuf buf) {
        super.write(buf);
        buf.writeBoolean(restart);
    }

    @Override
    public short getId() {
        return 1;
    }

    public boolean isRestart() {
        return restart;
    }
}
