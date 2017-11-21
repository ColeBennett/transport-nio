package net.thearchon.nio.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.thearchon.nio.BufferedPacket;
import net.thearchon.nio.protocol.Packet;
import net.thearchon.nio.protocol.PacketRegistry;
import net.thearchon.nio.protocol.impl.KeepAlivePacket;
import net.thearchon.nio.protocol.impl.RequestPacket;
import net.thearchon.nio.protocol.impl.ResponsePacket;
import net.thearchon.nio.protocol.stream.ChunkedFilePacket;

import java.util.List;

public class Decoder extends ByteToMessageDecoder {

    private static final int HEADER_LENGTH = 5;

    private final PacketRegistry registry;
    private ChunkedFilePacket currentStream;

    public Decoder(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (currentStream != null) {
            ChunkedFilePacket.ChunkedState state = currentStream.chunkReceived(buf);
            switch (state) {
                case STREAM_END:
                    currentStream = null;
                    break;
                case DECODE_NEXT:
                    currentStream = null;
                    decode(ctx, buf, out);
                    break;
                case BUFFER_EMPTY:
                    break;
            }
            return;
        }

        if (buf.readableBytes() < HEADER_LENGTH) {
            return;
        }
        buf.markReaderIndex();

        short id = buf.readShort();
        int bodyLen = buf.readUnsignedMedium();

        if (buf.readableBytes() < bodyLen) {
            buf.resetReaderIndex();
            return;
        }

        int startLen = buf.readableBytes();

        Packet packet;
        switch (id) {
            case KeepAlivePacket.PACKET_ID:
                packet = KeepAlivePacket.INSTANCE;
                out.add(packet);
                return;
            case RequestPacket.PACKET_ID:
                packet = new RequestPacket(registry);
                break;
            case ResponsePacket.PACKET_ID:
                packet = new ResponsePacket(registry);
                break;
            case BufferedPacket.PACKET_ID:
                packet = new BufferedPacket(buf);
                break;
            default:
                packet = registry.newInstance(id);
                break;
        }

        if (packet == null) {
            throw new DecoderException("Bad packet id: " + id);
        }
        if (id != BufferedPacket.PACKET_ID) {
            try {
                packet.read(buf);
            } catch (Throwable t) {
                buf.skipBytes(bodyLen);
                throw new DecoderException(t);
            }
            if (buf.readableBytes() != (startLen - bodyLen)) {
                buf.skipBytes(bodyLen);
                throw new DecoderException("Did not read all bytes from packet:"
                        + " (id: " + id + ", expected length: " + bodyLen + ")");
            }
        }

        if (packet instanceof ChunkedFilePacket) {
            ctx.pipeline().last().channelRead(ctx, packet);
            currentStream = (ChunkedFilePacket) packet;
            return;
        }
        out.add(packet);
    }
}
