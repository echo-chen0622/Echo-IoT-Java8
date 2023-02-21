package org.echoiot.server.common.transport;

import com.google.protobuf.ByteString;
import org.echoiot.server.common.transport.profile.TenantProfileUpdateResult;
import org.echoiot.server.common.data.TenantProfile;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface TransportTenantProfileCache {

    TenantProfile get(TenantId tenantId);

    TenantProfileUpdateResult put(ByteString profileBody);

    boolean put(TenantId tenantId, TenantProfileId profileId);

    @Nullable
    Set<TenantId> remove(TenantProfileId profileId);

}
