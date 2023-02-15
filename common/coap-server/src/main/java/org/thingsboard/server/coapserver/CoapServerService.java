package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;

public interface CoapServerService {

    CoapServer getCoapServer() throws UnknownHostException;

    ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> getDtlsSessionsMap();

    long getTimeout();

    long getPiggybackTimeout();

}
