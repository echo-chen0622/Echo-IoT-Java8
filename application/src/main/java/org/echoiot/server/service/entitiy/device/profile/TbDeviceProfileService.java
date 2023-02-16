package org.echoiot.server.service.entitiy.device.profile;

import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.exception.ThingsboardException;
import org.echoiot.server.service.entitiy.SimpleTbEntityService;

public interface TbDeviceProfileService extends SimpleTbEntityService<DeviceProfile> {

    DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws ThingsboardException;
}
