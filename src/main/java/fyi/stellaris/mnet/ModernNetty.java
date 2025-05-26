/*
 * This file is part of Modern Netty ~ https://git.gay/luciel/mc_mod_mnet
 * Copyright (C) 2025 ~ luciel
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

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.channel.uring.IoUringSocketChannel;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public final class ModernNetty {
  // @formatter:off
  public static final boolean
      IOURING = IoUring.isAvailable(),
      KQUEUE  = KQueue .isAvailable(),
      EPOLL   = Epoll  .isAvailable();

  public static final Class<LocalChannel> CHANNEL_CLIENT_LOCAL = LocalChannel.class;
  public static final Supplier<MultiThreadIoEventLoopGroup> GROUP_CLIENT_LOCAL = Suppliers.memoize(
      () -> new MultiThreadIoEventLoopGroup(
          new ThreadFactoryBuilder()
              .setNameFormat("Meow Client Local IO #%d")
              .setThreadFactory(Thread.ofPlatform().daemon(false).factory()).build(),
          LocalIoHandler.newFactory()
      ));

  public static final Class<? extends SocketChannel> CHANNEL_CLIENT =
      IOURING ? IoUringSocketChannel.class :
      KQUEUE  ? KQueueSocketChannel .class :
      EPOLL   ? EpollSocketChannel  .class :
                NioSocketChannel    .class ;

  public static final Supplier<MultiThreadIoEventLoopGroup> GROUP_CLIENT = Suppliers.memoize(
      () -> new MultiThreadIoEventLoopGroup(
          new ThreadFactoryBuilder()
              .setNameFormat("Meow Client IO #%d")
              .setThreadFactory(Thread.ofPlatform().factory()).build(),
          IOURING ? IoUringIoHandler.newFactory() :
          KQUEUE  ? KQueueIoHandler .newFactory() :
          EPOLL   ? EpollIoHandler  .newFactory() :
                    NioIoHandler    .newFactory()
      ));

  public static final Class<? extends ServerSocketChannel> CHANNEL_SERVER =
      IOURING ? IoUringServerSocketChannel.class :
      KQUEUE  ? KQueueServerSocketChannel .class :
      EPOLL   ? EpollServerSocketChannel  .class :
                NioServerSocketChannel    .class ;

  public static final Supplier<MultiThreadIoEventLoopGroup> GROUP_SERVER = Suppliers.memoize(
      () -> new MultiThreadIoEventLoopGroup(
          new ThreadFactoryBuilder()
              .setNameFormat("Meow Client IO #%d")
              .setThreadFactory(Thread.ofPlatform().daemon(false).factory()).build(),
          IOURING ? IoUringIoHandler.newFactory() :
          KQUEUE  ? KQueueIoHandler .newFactory() :
          EPOLL   ? EpollIoHandler  .newFactory() :
                    NioIoHandler    .newFactory()
      ));
  // @formatter:on

  private static final boolean IS_VFP_EXISTING = FabricLoader.getInstance().isModLoaded("viafabricplus");

  public static boolean shouldDefault() {
    return IS_VFP_EXISTING && com.viaversion.viafabricplus.ViaFabricPlus.getImpl()
        .getTargetVersion().equals(net.raphimc.viabedrock.api.BedrockProtocolVersion.bedrockLatest);
  }
}
