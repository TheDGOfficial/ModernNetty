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
package cc.luciel.mnet.injection.mixins.net.minecraft.network;

import cc.luciel.mnet.ModernNetty;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Connection.class, priority = 6969)
public abstract class MixinConnection {
  @Shadow
  @Final
  private static Logger LOGGER;

  @Redirect(method = "connect", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  private static AbstractBootstrap<Bootstrap, Channel> swapGroupClient(Bootstrap bootstrap, EventLoopGroup group) {
    if (ModernNetty.vfpIsBedrockSelected()) return bootstrap.group(group);
    return bootstrap
        .option(ChannelOption.AUTO_CLOSE, Boolean.TRUE)
        .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
        .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .group(ModernNetty.GROUP_CLIENT.get());
  }

  @Redirect(method = "connect", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  private static AbstractBootstrap<Bootstrap, Channel> swapChannelClient(Bootstrap bootstrap, Class<? extends Channel> clazz) {
    if (ModernNetty.vfpIsBedrockSelected()) return bootstrap.channel(clazz);
    return bootstrap.channel(ModernNetty.CHANNEL_CLIENT);
  }

  @Redirect(method = "connectToLocalServer", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  private static AbstractBootstrap<Bootstrap, Channel> swapGroupServer(Bootstrap bootstrap, EventLoopGroup group) {
    if (ModernNetty.vfpIsBedrockSelected()) return bootstrap.group(group);
    return bootstrap
        .option(ChannelOption.AUTO_CLOSE, Boolean.TRUE)
        .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
        .option(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .group(ModernNetty.GROUP_CLIENT_LOCAL.get());
  }

  @Redirect(method = "connectToLocalServer", at = @At(
      value = "INVOKE",
      target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
  ))
  private static AbstractBootstrap<Bootstrap, Channel> swapChannelServer(Bootstrap bootstrap, Class<? extends Channel> clazz) {
    if (ModernNetty.vfpIsBedrockSelected()) return bootstrap.channel(clazz);
    return bootstrap.channel(ModernNetty.CHANNEL_CLIENT_LOCAL);
  }


  @Inject(method = "exceptionCaught", at = @At(value = "HEAD"))
  private void injectExceptionCaught(ChannelHandlerContext context, Throwable t, CallbackInfo ci) {
    LOGGER.warn("netty exception", t);
  }
}
