package org.echoiot.server.dao.ota;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class OtaPackageCacheKey implements Serializable {

    @NotNull
    private final OtaPackageId id;

    @Override
    public String toString() {
        return id.toString();
    }

}
