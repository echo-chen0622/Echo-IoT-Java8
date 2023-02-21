package org.echoiot.server.dao.sql.device;

import org.echoiot.server.common.data.DeviceIdInfo;
import org.echoiot.server.common.data.page.PageData;
import org.springframework.data.domain.Pageable;

public interface NativeDeviceRepository {

    PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable);

}
