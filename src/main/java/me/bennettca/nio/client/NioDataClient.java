package me.bennettca.nio.client;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NioDataClient extends DataClient {

    public NioDataClient(ClientFutureListener future) {
        this(new NioEventLoopGroup(1,
                new DefaultThreadFactory("clientEventLoopGroup")), future);
    }

    public NioDataClient(NioEventLoopGroup group, ClientFutureListener future) {
        super(NioSocketChannel.class, group, future);
    }
}
