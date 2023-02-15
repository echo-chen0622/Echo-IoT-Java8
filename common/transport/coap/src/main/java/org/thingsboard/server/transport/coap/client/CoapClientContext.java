package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.transport.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

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
