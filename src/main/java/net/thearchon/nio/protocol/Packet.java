package net.thearchon.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public abstract class Packet {

    private final short id;

    public Packet(short id) {
        this.id = id;
    }

    public abstract void read(ByteBuf buf);

    public abstract void write(ByteBuf buf);

    public short getId() {
        return id;
    }

    public static byte[] readBytes(ByteBuf buf) {
        if (buf == null) {
            throw new NullPointerException("buffer");
        }
        int len = buf.readUnsignedMedium();
        byte[] arr = new byte[len];
        buf.readBytes(arr);
        return arr;
    }

    public static void writeBytes(ByteBuf buf, byte[] src) {
        if (buf == null) {
            throw new NullPointerException("buffer");
        }
        if (src == null) {
            throw new NullPointerException("src");
        }
        int len = src.length;
        if (len > Medium.MAX_UNSIGNED_VALUE) {
            throw new IllegalArgumentException(String.format("length: %d"
                    + " (expected: <= %d)", len, Medium.MAX_UNSIGNED_VALUE));
        }
        buf.ensureWritable(Medium.SIZE + len);
        buf.writeMedium(len);
        buf.writeBytes(src);
    }

    public static String readString(ByteBuf buf) {
        return new String(readBytes(buf), CharsetUtil.UTF_8);
    }

    public static void writeString(ByteBuf buf, String str) {
        writeBytes(buf, str.getBytes(CharsetUtil.UTF_8));
    }

    public static int utf8Length(CharSequence sequence) {
        return utf8Length(sequence, false);
    }

    public static int utf8Length(CharSequence sequence, boolean withHeaderBytes) {
        int count = withHeaderBytes ? Medium.SIZE : 0;
        for (int i = 0, len = sequence.length(); i < len; i++) {
            char c = sequence.charAt(i);
            if (c <= 0x7F) {
                count++;
            } else if (c <= 0x7FF) {
                count += 2;
            } else if (Character.isHighSurrogate(c)) {
                count += 4;
                ++i;
            } else {
                count += 3;
            }
        }
        return count;
    }
}
