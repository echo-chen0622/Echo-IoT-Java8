package org.thingsboard.server.common.transport.profile;

import lombok.Data;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Set;

@Data
public class TenantProfileUpdateResult {

    private final TenantProfile profile;
    private final Set<TenantId> affectedTenants;

}
