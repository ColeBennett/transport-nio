package me.bennettca.nio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPromise;
import me.bennettca.nio.protocol.PacketRegistry;
import me.bennettca.nio.protocol.packet.RequestPacket;
import me.bennettca.nio.request.ResponseHandler;
import me.bennettca.nio.protocol.Packet;

import java.net.InetSocketAddress;

public interface Connection {

    /**
     * Send packet to remote host.
     *
     * @param packet packet to send
     * @return ChannelFuture returned by underlying channel
     */
    ChannelFuture send(Packet packet);

    /**
     * Send packet to remote host.
     *
     * @param packet  packet to send
     * @param promise promise to be satisfied
     * @return ChannelFuture returned by underlying channel
     */
    ChannelFuture send(Packet packet, ChannelPromise promise);

    /**
     * Send multiple packets to remote host. Always use this method
     * instead of the single method. It executes send in the
     * io thread all at once, therefore is much faster and more efficient.
     *
     * @param packets packets to send
     */
    void send(Iterable<Packet> packets);

    /**
     * Write a packet to the outbound queue. Does not get sent to
     * remote host until flush is called.
     *
     * @param packet packet to write
     * @return ChannelFuture returned by underlying channel
     */
    ChannelFuture write(Packet packet);

    /**
     * Write a packet to the outbound queue. Does not get sent to
     * remote host until flush is called.
     *
     * @param packet  packet to write
     * @param promise promise to be satisfied
     * @return ChannelFuture returned by underlying channel
     */
    ChannelFuture write(Packet packet, ChannelPromise promise);

    /**
     * Request a packet to be sent and handled in the given handler.
     *
     * @param packet  packet to send
     * @param handler handler to be used
     * @return ChannelFuture returned by underlying channel
     */
    ChannelFuture request(Packet packet, ResponseHandler handler);

    ChannelFuture request(Packet packet,
            ResponseHandler handler, boolean timed);

    ChannelFuture respond(RequestPacket request, Packet response);

    void flush();

    ChannelFuture close();

    ChannelFuture close(ChannelPromise promise);

    ChannelFuture disconnect();

    ChannelFuture disconnect(ChannelPromise promise);

    boolean isActive();

    boolean isOpen();

    ByteBufAllocator alloc();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

    Channel getChannel();

    ChannelId getId();

    PacketRegistry getPacketRegistry();
}
