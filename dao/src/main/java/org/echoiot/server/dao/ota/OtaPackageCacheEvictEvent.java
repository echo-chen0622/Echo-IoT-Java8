package org.echoiot.server.dao.ota;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.OtaPackageId;

@Data
@RequiredArgsConstructor
class OtaPackageCacheEvictEvent {

    private final OtaPackageId id;

}
