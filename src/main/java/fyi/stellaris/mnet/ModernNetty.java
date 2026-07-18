/*
 * This file is part of Modern Netty ~ https://git.celesteflare.cc/stellaris/mod_mnet
 * Copyright (C) 2025 ~ iouring
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fyi.stellaris.mnet;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringChannelOption;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;
import io.netty.channel.uring.IoUringServerSocketChannel;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.foreign.ValueLayout.JAVA_INT;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;

public final class ModernNetty implements ModInitializer, ClientModInitializer {
    public static final boolean
            IOURING = IoUring.isAvailable(),
            KQUEUE = KQueue.isAvailable(),
            EPOLL = Epoll.isAvailable();

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ModernNetty.class);

    public static final Class<LocalChannel> CHANNEL_CLIENT_LOCAL =
            LocalChannel.class;

    public static final Supplier<MultiThreadIoEventLoopGroup>
            GROUP_CLIENT_LOCAL =
            LazyConstant.of(() ->
                    new MultiThreadIoEventLoopGroup(
                            Thread.ofPlatform()
                                    .daemon(true)
                                    .name("Netty Client Local IO #", 0)
                                    .factory(),
                            LocalIoHandler.newFactory()
                    ));

    public static final Class<? extends SocketChannel>
            CHANNEL_CLIENT =
            IOURING ? IoUringSocketChannel.class :
            KQUEUE ? KQueueSocketChannel.class :
            EPOLL ? EpollSocketChannel.class :
            NioSocketChannel.class;

    public static final Supplier<MultiThreadIoEventLoopGroup>
            GROUP_CLIENT =
            LazyConstant.of(() ->
                    new MultiThreadIoEventLoopGroup(
                            Thread.ofPlatform()
                                    .daemon(true)
                                    .name("Netty Client IO #", 0)
                                    .factory(),
                            IOURING ? IoUringIoHandler.newFactory() :
                            KQUEUE ? KQueueIoHandler.newFactory() :
                            EPOLL ? EpollIoHandler.newFactory() :
                            NioIoHandler.newFactory()
                    ));

    public static final Class<? extends ServerSocketChannel>
            CHANNEL_SERVER =
            IOURING ? IoUringServerSocketChannel.class :
            KQUEUE ? KQueueServerSocketChannel.class :
            EPOLL ? EpollServerSocketChannel.class :
            NioServerSocketChannel.class;

    public static final Supplier<MultiThreadIoEventLoopGroup>
            GROUP_SERVER =
            LazyConstant.of(() ->
                    new MultiThreadIoEventLoopGroup(
                            Thread.ofPlatform()
                                    .daemon(true)
                                    .name("Netty Server IO #", 0)
                                    .factory(),
                            IOURING ? IoUringIoHandler.newFactory() :
                            KQUEUE ? KQueueIoHandler.newFactory() :
                            EPOLL ? EpollIoHandler.newFactory() :
                            NioIoHandler.newFactory()
                    ));

    public static final int IPTOS_DSCP_EF = 0xb8;

    private static final int SOL_SOCKET = 1;
    private static final int SO_PRIORITY = 12;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP = LINKER.defaultLookup();

    private static final MethodHandle SETSOCKOPT;

    private static final MethodHandle INIT_CHANNEL_HANDLE;

    static {
        if (IOURING) {
            LOGGER.info(IoUring.featureString());
        }

        try {
            SETSOCKOPT = LINKER.downcallHandle(
                    LOOKUP.find("setsockopt").orElseThrow(),
                    FunctionDescriptor.of(
                            JAVA_INT,
                            JAVA_INT,
                            JAVA_INT,
                            JAVA_INT,
                            ValueLayout.ADDRESS,
                            JAVA_INT
                    )
            );
        } catch (final Throwable error) {
            throw new RuntimeException(error);
        }

        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(ChannelInitializer.class, lookup);

            INIT_CHANNEL_HANDLE = privateLookup.findVirtual(
                    ChannelInitializer.class,
                    "initChannel",
                    MethodType.methodType(void.class, Channel.class)
            );
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final void invokeInitChannel(final ChannelInitializer<Channel> initializer, final Channel channel) {
        try {
            INIT_CHANNEL_HANDLE.invokeExact(initializer, channel);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public ModernNetty() {
        super();
    }

    @Override
    public void onInitialize() {
        ModernNetty.init();
    }

    /**
     * This entrypoint is suitable for setting up client-specific logic, such as rendering.
     */
    @Override
    public final void onInitializeClient() {
        ModernNetty.init();
    }

    public static final void init() {
        // implicitly runs <clinit>

        ModernNettyChannelInitializer.init();
    }

    public static final void setPriority(final int fd, final int priority) {
        try (var arena = Arena.ofConfined()) {
            final MemorySegment optval = arena.allocate(JAVA_INT);
            optval.set(JAVA_INT, 0, priority);

            final int result = (int) SETSOCKOPT.invokeExact(
                    fd,
                    SOL_SOCKET,
                    SO_PRIORITY,
                    optval,
                    (int) JAVA_INT.byteSize()
            );

            if (-1 == result) {
                throw new RuntimeException("setsockopt(SO_PRIORITY) failed");
            }

        } catch (final Throwable error) {
            throw new RuntimeException(error);
        }
    }

    public static final void trySetSocketPriority(
            final Channel ch,
            final int priority
    ) {

        if (ch instanceof IoUringSocketChannel io) {
            ModernNetty.setPriority(io.fd().intValue(), priority);
        } else if (ch instanceof EpollSocketChannel epoll) {
            ModernNetty.setPriority(epoll.fd().intValue(), priority);
        }
    }

    @SuppressWarnings("unchecked")
    public static final AbstractBootstrap<Bootstrap, Channel> applyOptions(
            final Bootstrap bootstrap,
            final ChannelHandler existingHandler
    ) {
        if (!(existingHandler instanceof final ChannelInitializer<?> existingInitializer)) {
            throw new IllegalStateException("unexpected existing handler of type " + existingHandler.getClass().getName());
        }

        return bootstrap.handler(new ModernNetty.ModernNettyChannelInitializer((ChannelInitializer<Channel>) existingInitializer));
    }

    public static final AbstractBootstrap<Bootstrap, Channel> applyOptions(
            final Bootstrap bootstrap,
            final EventLoopGroup group
    ) {
        return ModernNetty.applyOptions(bootstrap, group, false);
    }

    public static final AbstractBootstrap<Bootstrap, Channel> applyOptions(
            final Bootstrap bootstrap,
            final EventLoopGroup group,
            final boolean forLocalChannel
    ) {
        var bs = null == group ? bootstrap : bootstrap
                    .group(group);

        if (!forLocalChannel) {
                bs = bs
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.TCP_FASTOPEN_CONNECT, true)
                        .option(ChannelOption.IP_TOS, ModernNetty.IPTOS_DSCP_EF);
        }

        return bs;
    }

    private static final class ModernNettyChannelInitializer extends ChannelInitializer<Channel> {
        private final ChannelInitializer<Channel> delegate;

        private static final void init() {
            // implicitly runs <clinit>
        }

        private ModernNettyChannelInitializer(final ChannelInitializer<Channel> delegate) {
            super();

            this.delegate = delegate;
        }

        @Override
        protected final void initChannel(final Channel ch) {
            if (ch instanceof IoUringSocketChannel) {
                ch.config().setOption(
                        IoUringChannelOption.TCP_QUICKACK,
                        true
                );
            } else if (ch instanceof EpollSocketChannel) {
                ch.config().setOption(
                        EpollChannelOption.TCP_QUICKACK,
                        true
                );
            }

            ModernNetty.trySetSocketPriority(ch, 6);

            ModernNetty.invokeInitChannel(this.delegate, ch);
        }
    }
}
