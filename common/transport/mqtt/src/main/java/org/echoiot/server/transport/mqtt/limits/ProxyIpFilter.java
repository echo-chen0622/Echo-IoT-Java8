package org.echoiot.server.transport.mqtt.limits;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.mqtt.MqttTransportContext;
import org.echoiot.server.transport.mqtt.MqttTransportService;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

@Slf4j
public class ProxyIpFilter extends ChannelInboundHandlerAdapter {


    private final MqttTransportContext context;

    public ProxyIpFilter(MqttTransportContext context) {
        this.context = context;
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("[{}] Received msg: {}", ctx.channel().id(), msg);
        if (msg instanceof HAProxyMessage) {
            @NotNull HAProxyMessage proxyMsg = (HAProxyMessage) msg;
            if (proxyMsg.sourceAddress() != null && proxyMsg.sourcePort() > 0) {
                @NotNull InetSocketAddress address = new InetSocketAddress(proxyMsg.sourceAddress(), proxyMsg.sourcePort());
                if (!context.checkAddress(address)) {
                    closeChannel(ctx);
                } else {
                    log.trace("[{}] Setting address: {}", ctx.channel().id(), address);
                    ctx.channel().attr(MqttTransportService.ADDRESS).set(address);
                    // We no longer need this channel in the pipeline. Similar to HAProxyMessageDecoder
                    ctx.pipeline().remove(this);
                }
            } else {
                log.trace("Received local health-check connection message: {}", proxyMsg);
                closeChannel(ctx);
            }
        }
    }

    private void closeChannel(@NotNull ChannelHandlerContext ctx) {
        while (ctx.pipeline().last() != this) {
            ChannelHandler handler = ctx.pipeline().removeLast();
            if (handler instanceof ChannelInboundHandlerAdapter) {
                try {
                    ((ChannelInboundHandlerAdapter) handler).channelUnregistered(ctx);
                } catch (Exception e) {
                    log.error("Failed to unregister channel: [{}]", ctx, e);
                }
            }

        }
        ctx.pipeline().remove(this);
        ctx.close();
    }
}
