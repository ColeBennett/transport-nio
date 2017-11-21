package me.bennettca.nio.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import me.bennettca.nio.EventHandler;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.PacketRegistry;
import me.bennettca.nio.protocol.codec.Decoder;
import me.bennettca.nio.protocol.codec.Encoder;
import me.bennettca.nio.protocol.packet.KeepAlivePacket;
import me.bennettca.nio.protocol.packet.RequestPacket;
import me.bennettca.nio.protocol.packet.ResponsePacket;
import me.bennettca.nio.request.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class DataServer {

    private final Map<ChannelId, ClientListener> clients;
    private final Collection<ClientListener> clientsView;

    private final PacketRegistry registry = new PacketRegistry();
    private final Set<EventHandler> handlers;
    final RequestPool requestPool;

    private final ChannelGroup channelGroup;
    private final ServerBootstrap bootstrap;
    private final SingleThreadEventExecutor executor;
    private final NioEventLoopGroup bossGroup, workerGroup;

    private ServerFutureListener future;
    private Channel serverChannel;
    private InetSocketAddress addr;
    private boolean closed;

    DataServer(Class<? extends ServerChannel> channelClass,
            NioEventLoopGroup bossGroup, NioEventLoopGroup workerGroup,
            ServerFutureListener future) {
        clients = new ConcurrentHashMap<>();
        clientsView = Collections.unmodifiableCollection(clients.values());

        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.future = future;

        handlers = new LinkedHashSet<>(1);
        executor = new DefaultEventExecutor();
        channelGroup = new DefaultChannelGroup(executor);
        requestPool = new RequestPool() {
            @Override
            protected void schedule(Runnable task) {
                DataServer.this.workerGroup.schedule(task, 10, TimeUnit.SECONDS);
            }

            @Override
            protected void requestFailed(final Request request) {
                executor.execute(() -> request.getHandler().requestFailed());
            }
        };

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(channelClass)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new ChunkedWriteHandler(),
                                new Decoder(registry),
                                Encoder.INSTANCE);
                        ch.pipeline().addLast(executor, new ServerHandler());
                    }
                });
    }

    public ChannelFuture bind(int port) {
        return bind(new InetSocketAddress(port));
    }

    public ChannelFuture bind(String host, int port) {
        return bind(new InetSocketAddress(host, port));
    }

    public ChannelFuture bind(InetAddress addr, int port) {
        return bind(new InetSocketAddress(addr, port));
    }

    public ChannelFuture bind(InetSocketAddress addr) {
        return bootstrap.bind(addr).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture f) {
                serverChannel = f.channel();
                DataServer.this.addr = addr;
                if (future != null) {
                    executor.execute(() -> {
                        if (f.isSuccess()) {
                            future.bindSucceeded();
                        } else {
                            future.bindFailed();
                        }
                    });
                }
            }
        });
    }

    public void close() {
        if (closed) {
            throw new IllegalStateException("Server has already been closed");
        }
        channelGroup.close();
        requestPool.clear();
        serverChannel.close();
        addr = null;
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        executor.shutdownGracefully();
        closed = true;
    }

    public ChannelGroupFuture sendAll(Packet packet) {
        if (packet == null) {
            throw new NullPointerException("packet");
        }
        return channelGroup.writeAndFlush(packet);
    }

    public ChannelGroupFuture sendAll(Packet packet, ChannelMatcher matcher) {
        if (packet == null) {
            throw new NullPointerException("packet");
        }
        return channelGroup.writeAndFlush(packet, matcher);
    }

    public void sendAll(Iterable<Packet> packets) {
        if (packets == null) {
            throw new NullPointerException("packets");
        }
        EventLoop exec = serverChannel.eventLoop();
        if (exec.inEventLoop()) {
            try {
                for (Packet packet : packets) {
                    registry.validate(packet);
                    channelGroup.write(packet);
                }
            } finally {
                channelGroup.flush();
            }
        } else {
            exec.execute(() -> sendAll(packets));
        }
    }

    public void sendAll(Iterable<Packet> packets, ChannelMatcher matcher) {
        if (packets == null) {
            throw new NullPointerException("packets");
        }
        EventLoop exec = serverChannel.eventLoop();
        if (exec.inEventLoop()) {
            try {
                for (Packet packet : packets) {
                    registry.validate(packet);
                    channelGroup.write(packet, matcher);
                }
            } finally {
                channelGroup.flush(matcher);
            }
        } else {
            exec.execute(() -> sendAll(packets, matcher));
        }
    }

    public ChannelGroupFuture disconnectAll() {
        return channelGroup.disconnect();
    }

    public ChannelGroupFuture disconnectAll(ChannelMatcher matcher) {
        return channelGroup.disconnect(matcher);
    }

    public ChannelGroupFuture closeAll() {
        return channelGroup.close();
    }

    public ChannelGroupFuture closeAll(ChannelMatcher matcher) {
        return channelGroup.close(matcher);
    }

    public boolean addHandler(EventHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        return handlers.add(handler);
    }

    public boolean removeHandler(EventHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        return handlers.remove(handler);
    }

    public SingleThreadEventExecutor getExecutor() {
        return executor;
    }

    public NioEventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public NioEventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public PacketRegistry getPacketRegistry() {
        return registry;
    }

    public InetSocketAddress getAddress() {
        return addr;
    }

    public boolean hasClient(Channel channel) {
        return hasClient(channel.id());
    }

    public boolean hasClient(ChannelId id) {
        return clients.containsKey(id);
    }

    public ClientListener getClient(Channel channel) {
        return getClient(channel.id());
    }

    public ClientListener getClient(ChannelId id) {
        return clients.get(id);
    }

    public int getClientCount() {
        return clients.size();
    }

    public Collection<ClientListener> getClients() {
        return clientsView;
    }

    public boolean isActive() {
        return serverChannel != null && serverChannel.isActive();
    }

    public boolean isOpen() {
        return !closed;
    }

    private void firePacketReceived(ClientListener channel, Packet packet) {
        if (packet.getId() != KeepAlivePacket.PACKET_ID) {
            for (EventHandler handler : handlers) {
                if (handler instanceof ServerEventHandler) {
                    ((ServerEventHandler) handler).packetReceived(channel, packet);
                }
            }
        }
    }

    @Sharable
    private final class ServerHandler extends ChannelHandlerAdapter {

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            ChannelId id = channel.id();
            channelGroup.add(channel);

            ClientListener client = new ClientListener(DataServer.this, channel);
            clients.put(id, client);
            for (EventHandler handler : handlers) {
                if (handler instanceof ServerEventHandler) {
                    ((ServerEventHandler) handler).clientConnected(client);
                }
            }
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            channelGroup.remove(channel);

            ClientListener client = clients.remove(channel.id());
            for (EventHandler handler : handlers) {
                if (handler instanceof ServerEventHandler) {
                    ((ServerEventHandler) handler).clientDisconnected(client);
                }
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (!(msg instanceof Packet)) {
                ctx.fireExceptionCaught(new UnsupportedMessageTypeException(msg.getClass().getSimpleName()));
                return;
            }
            Packet packet = (Packet) msg;
            Channel channel = ctx.channel();
            ClientListener client = getClient(channel);
            if (packet instanceof ResponsePacket) {
                ResponsePacket resp = (ResponsePacket) packet;
                Request request = requestPool.take(resp.getUniqueId());
                if (request != null) {
                    if (request instanceof TimedRequest) {
                        TimedRequest timed = (TimedRequest) request;
                        long time = System.currentTimeMillis() - timed.getStartTime();
                        request.getHandler().responseReceived(new TimedResponse(resp.getPacket(), time));
                    } else {
                        request.getHandler().responseReceived(new DefaultResponse(resp.getPacket()));
                    }
                } else {
                    ctx.fireExceptionCaught(new NullPointerException("Invalid response: " + resp.getUniqueId()));
                }
            } else if (packet instanceof RequestPacket) {
                RequestPacket request = (RequestPacket) packet;
                if (request.getPacket() instanceof KeepAlivePacket) {
                    client.respond(request, request.getPacket());
                } else {
                    firePacketReceived(client, packet);
                }
            } else {
                firePacketReceived(client, packet);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            e.getCause().printStackTrace();
        }
    }
}
