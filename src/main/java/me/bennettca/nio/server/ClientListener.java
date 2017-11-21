package me.bennettca.nio.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import me.bennettca.nio.AbstractConnection;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.PacketRegistry;
import me.bennettca.nio.protocol.packet.RequestPacket;
import me.bennettca.nio.request.DefaultRequest;
import me.bennettca.nio.request.Request;
import me.bennettca.nio.request.ResponseHandler;
import me.bennettca.nio.request.TimedRequest;

public final class ClientListener extends AbstractConnection {

    private final DataServer server;

    ClientListener(DataServer server, Channel channel) {
        this.server = server;
        this.channel = channel;
    }

    @Override
    public ChannelFuture request(Packet packet,
            ResponseHandler handler, boolean timed) {
        getPacketRegistry().validate(packet);
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        Request request = timed ? new TimedRequest(handler) : new DefaultRequest(handler);
        return send(new RequestPacket(server.requestPool.track(request, channel.eventLoop()), packet));
    }

    @Override
    public PacketRegistry getPacketRegistry() {
        return server.getPacketRegistry();
    }

    public DataServer getParent() {
        return server;
    }
}
