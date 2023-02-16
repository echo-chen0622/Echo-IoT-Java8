package org.echoiot.server.common.transport;

import com.google.protobuf.ByteString;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.id.DeviceProfileId;

public interface TransportDeviceProfileCache {

    DeviceProfile getOrCreate(DeviceProfileId id, ByteString profileBody);

    DeviceProfile get(DeviceProfileId id);

    void put(DeviceProfile profile);

    DeviceProfile put(ByteString profileBody);

    void evict(DeviceProfileId id);

}
