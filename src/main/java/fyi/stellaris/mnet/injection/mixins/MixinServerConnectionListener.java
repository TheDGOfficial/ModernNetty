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
package fyi.stellaris.mnet.injection.mixins;

import fyi.stellaris.mnet.ModernNetty;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerConnectionListener.class, priority = 6969)
public abstract class MixinServerConnectionListener {
  @Redirect(method = "startTcpServerListener", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/ServerBootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/ServerBootstrap;"
  ))
  public ServerBootstrap swapGroupServer(ServerBootstrap bootstrap, EventLoopGroup group) {
    return bootstrap.group(ModernNetty.shouldDefault() ? group : ModernNetty.GROUP_SERVER.get());
  }

  @Redirect(method = "startTcpServerListener", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  public AbstractBootstrap<ServerBootstrap, ServerChannel> swapChannelServer(ServerBootstrap bootstrap, Class<ServerChannel> clazz) {
    return bootstrap.channel(ModernNetty.shouldDefault() ? clazz : ModernNetty.CHANNEL_SERVER);
  }

  @Redirect(method = "startMemoryChannel", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/ServerBootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/ServerBootstrap;"
  ))
  public ServerBootstrap swapGroupChannels(ServerBootstrap bootstrap, EventLoopGroup group) {
    return bootstrap.group(ModernNetty.shouldDefault() ? group : ModernNetty.GROUP_CLIENT_LOCAL.get());
  }

  @Redirect(method = "startMemoryChannel", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  public AbstractBootstrap<ServerBootstrap, ServerChannel> swapChannelChannels(ServerBootstrap bootstrap, Class<ServerChannel> clazz) {
    return bootstrap.channel(ModernNetty.shouldDefault() ? clazz : LocalServerChannel.class);
  }
}
