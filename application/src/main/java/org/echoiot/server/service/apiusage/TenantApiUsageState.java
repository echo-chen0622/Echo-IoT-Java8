package org.echoiot.server.service.apiusage;

import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.common.data.tenant.profile.TenantProfileData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.util.Pair;

import java.util.*;

public class TenantApiUsageState extends BaseApiUsageState {
    @Getter
    @Setter
    private TenantProfileId tenantProfileId;
    @Getter
    @Setter
    private TenantProfileData tenantProfileData;

    public TenantApiUsageState(@NotNull TenantProfile tenantProfile, ApiUsageState apiUsageState) {
        super(apiUsageState);
        this.tenantProfileId = tenantProfile.getId();
        this.tenantProfileData = tenantProfile.getProfileData();
    }

    public TenantApiUsageState(ApiUsageState apiUsageState) {
        super(apiUsageState);
    }

    public long getProfileThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getProfileThreshold(key);
    }

    public long getProfileWarnThreshold(ApiUsageRecordKey key) {
        return tenantProfileData.getConfiguration().getWarnThreshold(key);
    }

    @Nullable
    private Pair<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(@NotNull ApiFeature feature) {
        @NotNull ApiUsageStateValue featureValue = ApiUsageStateValue.ENABLED;
        for (ApiUsageRecordKey recordKey : ApiUsageRecordKey.getKeys(feature)) {
            long value = get(recordKey);
            long threshold = getProfileThreshold(recordKey);
            long warnThreshold = getProfileWarnThreshold(recordKey);
            ApiUsageStateValue tmpValue;
            if (threshold == 0 || value == 0 || value < warnThreshold) {
                tmpValue = ApiUsageStateValue.ENABLED;
            } else if (value < threshold) {
                tmpValue = ApiUsageStateValue.WARNING;
            } else {
                tmpValue = ApiUsageStateValue.DISABLED;
            }
            featureValue = ApiUsageStateValue.toMoreRestricted(featureValue, tmpValue);
        }
        return setFeatureValue(feature, featureValue) ? Pair.of(feature, featureValue) : null;
    }


    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThresholds() {
        return checkStateUpdatedDueToThreshold(new HashSet<>(Arrays.asList(ApiFeature.values())));
    }

    @NotNull
    public Map<ApiFeature, ApiUsageStateValue> checkStateUpdatedDueToThreshold(@NotNull Set<ApiFeature> features) {
        @NotNull Map<ApiFeature, ApiUsageStateValue> result = new HashMap<>();
        for (@NotNull ApiFeature feature : features) {
            @Nullable Pair<ApiFeature, ApiUsageStateValue> tmp = checkStateUpdatedDueToThreshold(feature);
            if (tmp != null) {
                result.put(tmp.getFirst(), tmp.getSecond());
            }
        }
        return result;
    }

    @NotNull
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
