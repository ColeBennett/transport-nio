package me.bennettca.nio;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.PacketRegistry;
import me.bennettca.nio.protocol.packet.RequestPacket;
import me.bennettca.nio.protocol.packet.ResponsePacket;
import me.bennettca.nio.request.ResponseHandler;

import java.net.InetSocketAddress;

public abstract class AbstractConnection implements Connection {

    protected Channel channel;

    @Override
    public ChannelFuture send(Packet packet) {
        getPacketRegistry().validate(packet);
        checkActive();
        return channel.writeAndFlush(packet);
    }

    @Override
    public ChannelFuture send(Packet packet, ChannelPromise promise) {
        getPacketRegistry().validate(packet);
        checkActive();
        return channel.writeAndFlush(packet, promise);
    }

    @Override
    public void send(Iterable<Packet> packets) {
        if (packets == null) {
            throw new NullPointerException("packets");
        }
        checkActive();
        EventLoop exec = channel.eventLoop();
        if (exec.inEventLoop()) {
            PacketRegistry registry = getPacketRegistry();
            try {
                for (Packet packet : packets) {
                    registry.validate(packet);
                    channel.write(packet);
                }
            } finally {
                channel.flush();
            }
        } else {
            exec.execute(() -> send(packets));
        }
    }

    @Override
    public ChannelFuture write(Packet packet) {
        getPacketRegistry().validate(packet);
        checkActive();
        return channel.write(packet);
    }

    @Override
    public ChannelFuture write(Packet packet, ChannelPromise promise) {
        getPacketRegistry().validate(packet);
        checkActive();
        return channel.write(packet, promise);
    }

    @Override
    public ChannelFuture request(Packet packet, ResponseHandler handler) {
        return request(packet, handler, false);
    }

    @Override
    public ChannelFuture respond(RequestPacket request, Packet response) {
        if (request == null) {
            throw new NullPointerException("request");
        }
        getPacketRegistry().validate(response);
        return send(new ResponsePacket(request.getUniqueId(), response));
    }

    @Override
    public void flush() {
        checkActive();
        channel.flush();
    }

    @Override
    public ChannelFuture close() {
        checkActive();
        return channel.close();
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        checkActive();
        return channel.close(promise);
    }

    @Override
    public ChannelFuture disconnect() {
        checkActive();
        return channel.disconnect();
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        checkActive();
        return channel.disconnect(promise);
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public ByteBufAllocator alloc() {
        if (channel == null) {
            return null;
        }
        return channel.alloc();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        return (InetSocketAddress) channel.remoteAddress();
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public ChannelId getId() {
        if (channel == null) {
            return null;
        }
        return channel.id();
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Connection)) {
            return false;
        }
        return channel != null && ((Connection) o).getChannel().equals(channel);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + channel.toString() + ')';
    }

    private void checkActive() {
        if (channel == null) {
            throw new NullPointerException("channel");
        }
        if (!channel.isActive()) {
            throw new IllegalStateException("channel inactive");
        }
    }
}
