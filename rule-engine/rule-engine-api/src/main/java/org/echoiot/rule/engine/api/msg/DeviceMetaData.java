package org.echoiot.rule.engine.api.msg;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;

/**
 * Contains basic device metadata;
 *
 * @author ashvayka
 */
@Data
public final class DeviceMetaData {

    final DeviceId deviceId;
    final String deviceName;
    final String deviceType;
    final DeviceAttributes deviceAttributes;

}
