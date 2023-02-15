package org.thingsboard.server.common.msg;

import org.thingsboard.server.common.msg.aware.DeviceAwareMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;

import java.io.Serializable;

/**
 * @author Andrew Shvayka
 */
public interface ToDeviceActorNotificationMsg extends TbActorMsg, TenantAwareMsg, DeviceAwareMsg, Serializable {

}
