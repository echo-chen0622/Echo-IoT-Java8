package org.echoiot.rule.engine.api.msg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKey;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.msg.MsgType;
import org.echoiot.server.common.msg.ToDeviceActorNotificationMsg;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@ToString
@AllArgsConstructor
public class DeviceAttributesEventNotificationMsg implements ToDeviceActorNotificationMsg {

    @NotNull
    @Getter
    private final TenantId tenantId;
    @NotNull
    @Getter
    private final DeviceId deviceId;
    @NotNull
    @Getter
    private final Set<AttributeKey> deletedKeys;
    @NotNull
    @Getter
    private final String scope;
    @NotNull
    @Getter
    private final List<AttributeKvEntry> values;
    @Getter
    private final boolean deleted;

    @NotNull
    public static DeviceAttributesEventNotificationMsg onUpdate(TenantId tenantId, DeviceId deviceId, String scope, List<AttributeKvEntry> values) {
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, null, scope, values, false);
    }

    @NotNull
    public static DeviceAttributesEventNotificationMsg onDelete(TenantId tenantId, DeviceId deviceId, String scope, @NotNull List<String> keys) {
        @NotNull Set<AttributeKey> keysToNotify = new HashSet<>();
        keys.forEach(key -> keysToNotify.add(new AttributeKey(scope, key)));
        return new DeviceAttributesEventNotificationMsg(tenantId, deviceId, keysToNotify, null, null, true);
    }

    @NotNull
    @Override
    public MsgType getMsgType() {
        return MsgType.DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG;
    }
}
