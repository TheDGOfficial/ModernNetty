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
package fyi.stellaris.mnet.injection.mixins;

import fyi.stellaris.mnet.ModernNetty;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import net.minecraft.network.Connection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Connection.class, priority = 1049)
public final class MixinConnection {
    static {
        ModernNetty.init();
    }

    @Shadow
    @Final
    private static Logger LOGGER;

    @Redirect(
            method = "connect",
            at = @At(
                    value = "INVOKE",
                    target =
                    "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"
            ),
            require = 0
    )
    private static final AbstractBootstrap<Bootstrap, Channel>
    connect$group(
            final Bootstrap bootstrap,
            final EventLoopGroup group
    ) {
        return ModernNetty.applyOptions(
                bootstrap,
                ModernNetty.GROUP_CLIENT.get()
        );
    }

    @Redirect(
            method = "connect",
            at = @At(
                    value = "INVOKE",
                    target =
                    "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
            ),
            require = 0
    )
    private static final AbstractBootstrap<Bootstrap, Channel>
    connect$channel(
            final Bootstrap bootstrap,
            final Class<? extends Channel> clazz
    ) {
        return bootstrap.channel(ModernNetty.CHANNEL_CLIENT);
    }

    @Redirect(
        method = "connect",
        at = @At(
            value = "INVOKE",
            target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;"
        ),
        require = 0
    )
    private static final AbstractBootstrap<Bootstrap, Channel>
    connect$handler(
            final Bootstrap bootstrap,
            final ChannelHandler handler
    ) {
        return ModernNetty.applyOptions(
                bootstrap,
                handler
        );
    }

    @Redirect(
            method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V",
            at = @At(
                    value = "INVOKE",
                    target =
                    "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;"
            ),
            require = 0
    )
    private static final ChannelFuture disconnect$awaitUninterruptibly(
            final ChannelFuture instance
    ) {
        return instance;
    }

    @Redirect(
            method = "connectToLocalServer",
            at = @At(
                    value = "INVOKE",
                    target =
                    "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;"
            )
    )
    private static final AbstractBootstrap<Bootstrap, Channel>
    connectToLocalServer$group(
            final Bootstrap bootstrap,
            final EventLoopGroup group
    ) {
        return ModernNetty.applyOptions(
                bootstrap,
                ModernNetty.GROUP_CLIENT_LOCAL.get(),
                true
        );
    }

    @Redirect(
            method = "connectToLocalServer",
            at = @At(
                    value = "INVOKE",
                    target =
                    "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;"
            )
    )
    private static final AbstractBootstrap<Bootstrap, Channel>
    connectToLocalServer$channel(
            final Bootstrap bootstrap,
            final Class<? extends Channel> clazz
    ) {
        return bootstrap.channel(
                ModernNetty.CHANNEL_CLIENT_LOCAL
        );
    }

    @Inject(
            method = "exceptionCaught",
            at = @At("HEAD")
    )
    private final void exceptionCaught(
            final ChannelHandlerContext context,
            final Throwable throwable,
            final CallbackInfo ci
    ) {
        LOGGER.warn("netty exception", throwable);
    }
}
