package org.echoiot.server.common.msg;

import org.echoiot.server.common.msg.aware.DeviceAwareMsg;
import org.echoiot.server.common.msg.aware.TenantAwareMsg;

import java.io.Serializable;

/**
 * @author Echo
 */
public interface ToDeviceActorNotificationMsg extends TbActorMsg, TenantAwareMsg, DeviceAwareMsg, Serializable {

}
