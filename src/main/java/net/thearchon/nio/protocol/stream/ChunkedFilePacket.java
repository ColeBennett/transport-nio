package net.thearchon.nio.protocol.stream;

import io.netty.buffer.ByteBuf;
import io.netty.handler.stream.ChunkedNioFile;
import net.thearchon.nio.protocol.LengthAwarePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;

public class ChunkedFilePacket extends LengthAwarePacket {

    public static final short PACKET_ID = -2;

    public enum ChunkedState {
        STREAM_END, DECODE_NEXT, BUFFER_EMPTY
    }

    private String name;
    private File src, dst;
    private ChunkedStream stream;

    private ChunkedNioFile nioChunks;
    private FileOutputStream fos;
    private FileChannel fc;
    private StreamCompletionHandler handler;

    /**
     * Empty constructor used in the decoder.
     */
    public ChunkedFilePacket() {
        super(PACKET_ID);
    }

    /**
     * Construct with file parameter.
     *
     * @param file the file to be read from
     */
    public ChunkedFilePacket(File file) {
        this();

        if (file == null) {
            throw new NullPointerException("file");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("File cannot be a directory");
        }
        if (!file.exists()) {
            throw new IllegalStateException(file.getAbsolutePath());
        }
        src = file;
    }

    @Override
    public void read(ByteBuf buf) {
        name = readString(buf);
        stream = new ChunkedStream(buf.readLong());
    }

    @Override
    public void write(ByteBuf buf) {
        writeString(buf, src.getName());
        buf.writeLong(src.length());

        try {
            nioChunks = new ChunkedNioFile(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int length() {
        return utf8Length(src.getName(), true) + 8;
    }

    public String getFileName() {
        return name;
    }

    public File getDstFile() {
        return dst;
    }

    public ChunkedStream getStream() {
        return stream;
    }

    public ChunkedNioFile getChunks() {
        return nioChunks;
    }

    public void setCompletionHandler(StreamCompletionHandler handler) {
        this.handler = handler;
    }

    public void setDstFile(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }

        if (file.isDirectory()) {
            dst = new File(file, name);
        } else {
            dst = file;
        }
        if (dst.exists()) {
            dst.delete();
        }

        File parent = dst.getParentFile();
        if (parent != null) {
            parent.mkdir();
        }
        try {
            dst.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fos = new FileOutputStream(dst, true);
            fc = fos.getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (fc != null) {
                fc.close();
                fc = null;
            }
            if (fos != null) {
                fos.close();
                fos = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChunkedState chunkReceived(ByteBuf data) {
        int size = data.readableBytes();
        ChunkedState state;
        if ((stream.getProgress() + size) > stream.getTotal()) {
            size -= (stream.getProgress() + size) - stream.getTotal();
            state = ChunkedState.DECODE_NEXT;
        } else {
            state = ChunkedState.BUFFER_EMPTY;
        }

        try {
            if (fc != null) {
                fc.write(data.nioBuffer());
            } else {
                System.out.println("Destination file not set for file: " + name);
            }
            stream.bytesTransferred(size);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            data.skipBytes(size);
        }

        if (stream.isComplete()) {
            close();
            System.out.println("Operation complete (" + (stream.getTimeCompleted() / 1000) + " sec): " + dst + " (bytes transferred: " + stream.getTotal() + ", chunks received: " + stream.getChunksReceived() + ")");
            state = ChunkedState.STREAM_END;
            if (handler != null) {
                handler.streamCompleted();
            }
        }
        return state;
    }

    private String transferred() {
        return sizeDisplay(stream.getProgress());
    }

    static final int div = 1000;
    static final DecimalFormat decFormat = new DecimalFormat("###.#");

    public static String sizeDisplay(long count) {
        double d = (double) count;
        if ((d / div / div / div / div) >= 1) {
            double dec = d / div / div / div / div;
            return decFormat.format(dec) + " TB";
        } else if ((d / div / div / div) >= 1) {
            double dec = d / div / div / div;
            return decFormat.format(dec) + " GB";
        } else if ((d / div / div) >= 1) {
            double dec = d / div / div;
            return decFormat.format(dec) + " MB";
        } else if ((d / div) >= 1) {
            double dec = d / div;
            return decFormat.format(dec) + " KB";
        } else {
            return count + " Bytes";
        }
    }

    private void printProgress() {
        StringBuilder buf = new StringBuilder(102);
        int progress = (int) stream.calculateProgress();
        buf.append("Progress (~").append(progress).append("%) ");
        buf.append('[');
        for (int i = 1; i <= 100; i++) {
            buf.append(i <= progress ? '#' : '_');
        }
        buf.append("] ");
        buf.append(transferred());
        System.out.println(buf);
    }

    @Override
    public String toString() {
        return "ChunkedFilePacket("
                + "file: " + name
                + ", progress: " + (stream != null ? stream.getProgress() : "null")
                + ", total: " + (stream != null ? stream.getTotal() : "null") + ')';
    }
}
