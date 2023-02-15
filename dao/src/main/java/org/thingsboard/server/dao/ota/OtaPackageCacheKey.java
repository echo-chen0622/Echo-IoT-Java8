package org.thingsboard.server.dao.ota;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
@Builder
public class OtaPackageCacheKey implements Serializable {

    private final OtaPackageId id;

    @Override
    public String toString() {
        return id.toString();
    }

}
