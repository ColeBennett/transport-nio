package me.bennettca.nio.server;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NioDataServer extends DataServer {

    public NioDataServer(ServerFutureListener future) {
        super(NioServerSocketChannel.class,
                new NioEventLoopGroup(1, new DefaultThreadFactory("serverBossGroup")),
                new NioEventLoopGroup(1, new DefaultThreadFactory("serverWorkerGroup")), future);
    }
}
