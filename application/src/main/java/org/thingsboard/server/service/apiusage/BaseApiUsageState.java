/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.apiusage;

import lombok.Getter;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseApiUsageState {
    private final Map<ApiUsageRecordKey, Long> currentCycleValues = new ConcurrentHashMap<>();
    private final Map<ApiUsageRecordKey, Long> currentHourValues = new ConcurrentHashMap<>();

    @Getter
    private final ApiUsageState apiUsageState;
    @Getter
    private volatile long currentCycleTs;
    @Getter
    private volatile long nextCycleTs;
    @Getter
    private volatile long currentHourTs;

    public BaseApiUsageState(ApiUsageState apiUsageState) {
        this.apiUsageState = apiUsageState;
        this.currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        this.nextCycleTs = SchedulerUtils.getStartOfNextMonth();
        this.currentHourTs = SchedulerUtils.getStartOfCurrentHour();
    }

    public void put(ApiUsageRecordKey key, Long value) {
        currentCycleValues.put(key, value);
    }

    public void putHourly(ApiUsageRecordKey key, Long value) {
        currentHourValues.put(key, value);
    }

    public long add(ApiUsageRecordKey key, long value) {
        long result = currentCycleValues.getOrDefault(key, 0L) + value;
        currentCycleValues.put(key, result);
        return result;
    }

    public long get(ApiUsageRecordKey key) {
        return currentCycleValues.getOrDefault(key, 0L);
    }

    public long addToHourly(ApiUsageRecordKey key, long value) {
        long result = currentHourValues.getOrDefault(key, 0L) + value;
        currentHourValues.put(key, result);
        return result;
    }

    public void setHour(long currentHourTs) {
        this.currentHourTs = currentHourTs;
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            currentHourValues.put(key, 0L);
        }
    }

    public void setCycles(long currentCycleTs, long nextCycleTs) {
        this.currentCycleTs = currentCycleTs;
        this.nextCycleTs = nextCycleTs;
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            currentCycleValues.put(key, 0L);
        }
    }

    public ApiUsageStateValue getFeatureValue(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return apiUsageState.getTransportState();
            case RE:
                return apiUsageState.getReExecState();
            case DB:
                return apiUsageState.getDbStorageState();
            case JS:
                return apiUsageState.getJsExecState();
            case EMAIL:
                return apiUsageState.getEmailExecState();
            case SMS:
                return apiUsageState.getSmsExecState();
            case ALARM:
                return apiUsageState.getAlarmExecState();
            default:
                return ApiUsageStateValue.ENABLED;
        }
    }

    public boolean setFeatureValue(ApiFeature feature, ApiUsageStateValue value) {
        ApiUsageStateValue currentValue = getFeatureValue(feature);
        switch (feature) {
            case TRANSPORT:
                apiUsageState.setTransportState(value);
                break;
            case RE:
                apiUsageState.setReExecState(value);
                break;
            case DB:
                apiUsageState.setDbStorageState(value);
                break;
            case JS:
                apiUsageState.setJsExecState(value);
                break;
            case EMAIL:
                apiUsageState.setEmailExecState(value);
                break;
            case SMS:
                apiUsageState.setSmsExecState(value);
                break;
            case ALARM:
                apiUsageState.setAlarmExecState(value);
                break;
        }
        return !currentValue.equals(value);
    }

    public abstract EntityType getEntityType();

    public TenantId getTenantId() {
        return getApiUsageState().getTenantId();
    }

    public EntityId getEntityId() {
        return getApiUsageState().getEntityId();
    }
}
