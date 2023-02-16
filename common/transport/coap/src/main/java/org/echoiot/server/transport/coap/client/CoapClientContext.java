package org.echoiot.server.transport.coap.client;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.msg.session.SessionMsgType;
import org.echoiot.server.common.transport.adaptor.AdaptorException;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.concurrent.atomic.AtomicInteger;

public interface CoapClientContext {

    boolean registerAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    boolean registerRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    AtomicInteger getNotificationCounterByToken(String token);

    TbCoapClientState getOrCreateClient(SessionMsgType type, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) throws AdaptorException;

    TransportProtos.SessionInfoProto getNewSyncSession(TbCoapClientState clientState);

    void deregisterAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    void deregisterRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    void reportActivity();

    void registerObserveRelation(String token, ObserveRelation relation);

    void deregisterObserveRelation(String token);

    boolean awake(TbCoapClientState client);
}
