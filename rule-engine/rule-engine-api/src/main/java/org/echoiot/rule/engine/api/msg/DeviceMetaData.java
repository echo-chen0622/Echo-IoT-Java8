package org.echoiot.rule.engine.api.msg;

import lombok.Data;
import org.echoiot.server.common.data.id.DeviceId;
import org.jetbrains.annotations.NotNull;

/**
 * Contains basic device metadata;
 *
 * @author ashvayka
 */
@Data
public final class DeviceMetaData {

    @NotNull
    final DeviceId deviceId;
    @NotNull
    final String deviceName;
    @NotNull
    final String deviceType;
    @NotNull
    final DeviceAttributes deviceAttributes;

}
