package org.echoiot.server.transport.mqtt.limits;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.transport.mqtt.MqttTransportContext;
import org.echoiot.server.transport.mqtt.MqttTransportService;

import java.net.InetSocketAddress;

@Slf4j
public class IpFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {

    private final MqttTransportContext context;

    public IpFilter(MqttTransportContext context) {
        this.context = context;
    }

    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
        log.trace("[{}] Received msg: {}", ctx.channel().id(), remoteAddress);
        if(context.checkAddress(remoteAddress)){
            log.trace("[{}] Setting address: {}", ctx.channel().id(), remoteAddress);
            ctx.channel().attr(MqttTransportService.ADDRESS).set(remoteAddress);
            return true;
        } else {
            return false;
        }
    }
}
