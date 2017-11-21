package me.bennettca.nio.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import me.bennettca.nio.AbstractConnection;
import me.bennettca.nio.EventHandler;
import me.bennettca.nio.protocol.PacketRegistry;
import me.bennettca.nio.protocol.codec.Decoder;
import me.bennettca.nio.protocol.packet.RequestPacket;
import me.bennettca.nio.protocol.packet.ResponsePacket;
import me.bennettca.nio.request.*;
import me.bennettca.nio.protocol.Packet;
import me.bennettca.nio.protocol.codec.Encoder;
import me.bennettca.nio.protocol.packet.KeepAlivePacket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class DataClient extends AbstractConnection {

    private final Bootstrap bootstrap;
    private final RequestPool requestPool;
    private final PacketRegistry registry = new PacketRegistry();
    private final Set<EventHandler> handlers;

    private EventLoopGroup group;
    private ClientFutureListener future;

    private InetSocketAddress addr;
    private long reconnectDelay;
    private boolean reconnectFlag;
    private ReconnectHandler reconnectHandler;

    DataClient(Class<? extends SocketChannel> channelClass,
            EventLoopGroup group, ClientFutureListener future) {
        this.group = group;
        this.future = future;

        handlers = new LinkedHashSet<>(1);
        requestPool = new RequestPool() {
            @Override
            protected void schedule(Runnable task) {
                DataClient.this.group.schedule(task, 10, TimeUnit.SECONDS);
            }
        };

        bootstrap = new Bootstrap()
                .group(group)
                .channel(channelClass)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new IdleStateHandler(30, 15, 0),
                                new ChunkedWriteHandler(),
                                new Decoder(registry),
                                Encoder.INSTANCE,
                                new Handler());
                    }
                });
    }

    @Override
    public ChannelFuture request(Packet packet,
            ResponseHandler handler, boolean timed) {
        getPacketRegistry().validate(packet);
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        Request request = timed ? new TimedRequest(handler) : new DefaultRequest(handler);
        return send(new RequestPacket(requestPool.track(request, channel.eventLoop()), packet));
    }

    @Override
    public ChannelFuture close() {
        ChannelFuture close = super.close();
        group.shutdownGracefully();
        return close;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        ChannelFuture close = super.close(promise);
        group.shutdownGracefully();
        return close;
    }

    private final ChannelFutureListener listener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture f) throws Exception {
            if (f.isSuccess()) {
                channel = f.channel();
                addr = (InetSocketAddress) channel.remoteAddress();
                if (future != null) {
                    future.clientConnected();
                }
                if (reconnectHandler != null && reconnectFlag) {
                    reconnectHandler.clientReconnected();
                }
            } else {
                if (future != null) {
                    future.connectFailed();
                }
                attemptReconnect(addr);
            }
        }
    };

    public ChannelFuture connect(String host, int port) {
        return connect(new InetSocketAddress(host, port));
    }

    public ChannelFuture connect(InetAddress addr, int port) {
        return connect(new InetSocketAddress(addr, port));
    }

    public ChannelFuture connect(InetSocketAddress addr) {
        if (isActive()) {
            throw new ChannelException("Connection already active: " + getRemoteAddress());
        }
        this.addr = addr;
        return bootstrap.connect(addr).addListener(listener);
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

    public PacketRegistry getPacketRegistry() {
        return registry;
    }

    public EventLoopGroup getExecutor() {
        return group;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelayMillis(long reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public void setReconnectHandler(ReconnectHandler reconnectHandler) {
        this.reconnectHandler = reconnectHandler;
    }

    private void attemptReconnect(final InetSocketAddress addr) {
        if (reconnectDelay > 0) {
            group.schedule((Runnable) () -> {
                if (!isActive()) {
                    reconnectFlag = true;
                    connect(addr);
                }
            }, reconnectDelay, TimeUnit.MILLISECONDS);
        }
    }

    private void firePacketReceived(Packet packet) {
        if (packet.getId() != KeepAlivePacket.PACKET_ID) {
            for (EventHandler handler : handlers) {
                if (handler instanceof ClientEventHandler) {
                    ((ClientEventHandler) handler).packetReceived(packet);
                }
            }
        }
    }

    private final class Handler extends ChannelHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idle = (IdleStateEvent) evt;
                if (idle.state() == IdleState.WRITER_IDLE) {
                    ctx.writeAndFlush(KeepAlivePacket.INSTANCE);
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            requestPool.clear();
            for (EventHandler handler : handlers) {
                if (handler instanceof ClientEventHandler) {
                    ((ClientEventHandler) handler).connectionLost();
                }
            }

            channel = null;
            InetSocketAddress remote = addr;
            addr = null;
            attemptReconnect(remote);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof Packet)) {
                ctx.fireExceptionCaught(new UnsupportedMessageTypeException(msg.getClass().getSimpleName()));
                return;
            }
            Packet packet = (Packet) msg;
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
                    respond(request, request.getPacket());
                } else {
                    firePacketReceived(packet);
                }
            } else {
                firePacketReceived(packet);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
            e.printStackTrace();
        }
    }
}
