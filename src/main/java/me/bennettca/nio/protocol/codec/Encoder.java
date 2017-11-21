package me.bennettca.nio.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import me.bennettca.nio.protocol.Medium;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.packet.KeepAlivePacket;
import me.bennettca.nio.protocol.LengthAwarePacket;
import me.bennettca.nio.protocol.stream.ChunkedFilePacket;

@Sharable
public final class Encoder extends ChannelHandlerAdapter {

    public static final Encoder INSTANCE = new Encoder();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof KeepAlivePacket) {
            ByteBuf buf = ctx.alloc().buffer(5);
            buf.writeShort(KeepAlivePacket.PACKET_ID);
            buf.writeMedium(0);
            ctx.writeAndFlush(buf, promise);
            return;
        }
        short id;
        ByteBuf data;
        if (msg instanceof Packet) {
            Packet packet = (Packet) msg;
            if (packet instanceof LengthAwarePacket) {
                int len = ((LengthAwarePacket) packet).length();
                if (len < 0) {
                    throw new EncoderException("packet length < 0");
                }
                data = ctx.alloc().buffer(len, len);
            } else {
                data = ctx.alloc().buffer();
            }
            boolean success = false;
            try {
                packet.write(data);
                success = true;
            } catch (Exception e) {
                ctx.fireExceptionCaught(new EncoderException("An error occured"
                        + " while encoding packet: " + packet, e));
            } finally {
                if (!success) {
                    data.release();
                }
            }
            id = packet.getId();
        } else if (msg instanceof ByteBuf) {
            id = 0;
            data = (ByteBuf) msg;
        } else {
            ctx.fireExceptionCaught(new UnsupportedMessageTypeException(msg.getClass().getSimpleName()));
            return;
        }

        int bodyLen = data.readableBytes();
        if (bodyLen > Medium.MAX_UNSIGNED_VALUE) {
            ctx.fireExceptionCaught(new TooLongFrameException(String.format("length: %d"
                    + " (expected: <= %d)", bodyLen, Medium.MAX_UNSIGNED_VALUE)));
            return;
        }

        int frameLen = bodyLen + 5;
        ByteBuf out = ctx.alloc().buffer(frameLen, frameLen);
        out.writeShort(id);
        out.writeMedium(bodyLen);
        out.writeBytes(data);
        data.release();

        ctx.write(out, promise);
        if (msg instanceof ChunkedFilePacket) {
            ctx.write(((ChunkedFilePacket) msg).getChunks());
        }
        ctx.flush();
    }

    private Encoder() {

    }
}
