package org.thingsboard.server.dao.ota;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.OtaPackageId;

@Data
@RequiredArgsConstructor
class OtaPackageCacheEvictEvent {

    private final OtaPackageId id;

}
