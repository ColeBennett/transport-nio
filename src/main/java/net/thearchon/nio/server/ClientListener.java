package net.thearchon.nio.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.thearchon.nio.AbstractConnection;
import net.thearchon.nio.protocol.Packet;
import net.thearchon.nio.protocol.PacketRegistry;
import net.thearchon.nio.protocol.impl.RequestPacket;
import net.thearchon.nio.request.DefaultRequest;
import net.thearchon.nio.request.Request;
import net.thearchon.nio.request.ResponseHandler;
import net.thearchon.nio.request.TimedRequest;

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
