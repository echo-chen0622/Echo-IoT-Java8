package org.echoiot.server.common.transport.profile;

import lombok.Data;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Data
public class TenantProfileUpdateResult {

    @NotNull
    private final TenantProfile profile;
    @NotNull
    private final Set<TenantId> affectedTenants;

}
