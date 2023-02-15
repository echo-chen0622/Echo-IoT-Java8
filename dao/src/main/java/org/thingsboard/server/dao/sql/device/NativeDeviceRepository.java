package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.page.PageData;

public interface NativeDeviceRepository {

    PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable);

}
