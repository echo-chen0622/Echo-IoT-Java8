package org.echoiot.server.dao.ota;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor
class OtaPackageCacheEvictEvent {

    @NotNull
    private final OtaPackageId id;

}
