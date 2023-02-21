package org.echoiot.rule.engine.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.RuleEngineDeviceProfileCache;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.rule.engine.api.util.TbNodeUtils;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.rule.RuleNodeState;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.echoiot.server.common.msg.queue.PartitionChangeMsg;
import org.echoiot.common.util.JacksonUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "device profile",
        customRelations = true,
        relationTypes = {"Alarm Created", "Alarm Updated", "Alarm Severity Updated", "Alarm Cleared", "Success", "Failure"},
        configClazz = TbDeviceProfileNodeConfiguration.class,
        nodeDescription = "Process device messages based on device profile settings",
        nodeDetails = "Create and clear alarms based on alarm rules defined in device profile. The output relation type is either " +
                "'Alarm Created', 'Alarm Updated', 'Alarm Severity Updated' and 'Alarm Cleared' or simply 'Success' if no alarms were affected.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbDeviceProfileConfig"
)
public class TbDeviceProfileNode implements TbNode {
    private static final String PERIODIC_MSG_TYPE = "TbDeviceProfilePeriodicMsg";
    private static final String PROFILE_UPDATE_MSG_TYPE = "TbDeviceProfileUpdateMsg";
    private static final String DEVICE_UPDATE_MSG_TYPE = "TbDeviceUpdateMsg";

    private TbDeviceProfileNodeConfiguration config;
    private RuleEngineDeviceProfileCache cache;
    private TbContext ctx;
    private final Map<DeviceId, DeviceState> deviceStates = new ConcurrentHashMap<>();

    @Override
    public void init(@NotNull TbContext ctx, @NotNull TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbDeviceProfileNodeConfiguration.class);
        this.cache = ctx.getDeviceProfileCache();
        this.ctx = ctx;
        scheduleAlarmHarvesting(ctx, null);
        ctx.addDeviceProfileListeners(this::onProfileUpdate, this::onDeviceUpdate);
        if (config.isFetchAlarmRulesStateOnStart()) {
            log.info("[{}] Fetching alarm rule state", ctx.getSelfId());
            int fetchCount = 0;
            PageLink pageLink = new PageLink(1024);
            while (true) {
                PageData<RuleNodeState> states = ctx.findRuleNodeStates(pageLink);
                if (!states.getData().isEmpty()) {
                    for (@NotNull RuleNodeState rns : states.getData()) {
                        fetchCount++;
                        if (rns.getEntityId().getEntityType().equals(EntityType.DEVICE) && ctx.isLocalEntity(rns.getEntityId())) {
                            getOrCreateDeviceState(ctx, new DeviceId(rns.getEntityId().getId()), rns);
                        }
                    }
                }
                if (!states.hasNext()) {
                    break;
                } else {
                    pageLink = pageLink.nextPageLink();
                }
            }
            log.info("[{}] Fetched alarm rule state for {} entities", ctx.getSelfId(), fetchCount);
        }
        if (!config.isPersistAlarmRulesState() && ctx.isLocalEntity(ctx.getSelfId())) {
            log.debug("[{}] Going to cleanup rule node states", ctx.getSelfId());
            ctx.clearRuleNodeStates();
        }
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, @NotNull TbMsg msg) throws ExecutionException, InterruptedException {
        EntityType originatorType = msg.getOriginator().getEntityType();
        if (msg.getType().equals(PERIODIC_MSG_TYPE)) {
            scheduleAlarmHarvesting(ctx, msg);
            harvestAlarms(ctx, System.currentTimeMillis());
        } else if (msg.getType().equals(PROFILE_UPDATE_MSG_TYPE)) {
            updateProfile(ctx, new DeviceProfileId(UUID.fromString(msg.getData())));
        } else if (msg.getType().equals(DEVICE_UPDATE_MSG_TYPE)) {
            JsonNode data = JacksonUtil.toJsonNode(msg.getData());
            @NotNull DeviceId deviceId = new DeviceId(UUID.fromString(data.get("deviceId").asText()));
            if (data.has("profileId")) {
                invalidateDeviceProfileCache(deviceId, new DeviceProfileId(UUID.fromString(data.get("deviceProfileId").asText())));
            } else {
                removeDeviceState(deviceId);
            }
        } else {
            if (EntityType.DEVICE.equals(originatorType)) {
                @NotNull DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
                if (msg.getType().equals(DataConstants.ENTITY_UPDATED)) {
                    invalidateDeviceProfileCache(deviceId, msg.getData());
                    ctx.tellSuccess(msg);
                } else if (msg.getType().equals(DataConstants.ENTITY_DELETED)) {
                    removeDeviceState(deviceId);
                    ctx.tellSuccess(msg);
                } else {
                    @Nullable DeviceState deviceState = getOrCreateDeviceState(ctx, deviceId, null);
                    if (deviceState != null) {
                        deviceState.process(ctx, msg);
                    } else {
                        log.info("Device was not found! Most probably device [" + deviceId + "] has been removed from the database. Acknowledging msg.");
                        ctx.ack(msg);
                    }
                }
            } else {
                ctx.tellSuccess(msg);
            }
        }
    }

    @Override
    public void onPartitionChangeMsg(@NotNull TbContext ctx, PartitionChangeMsg msg) {
        // Cleanup the cache for all entities that are no longer assigned to current server partitions
        deviceStates.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
    }

    @Override
    public void destroy() {
        ctx.removeListeners();
        deviceStates.clear();
    }

    @Nullable
    protected DeviceState getOrCreateDeviceState(@NotNull TbContext ctx, DeviceId deviceId, RuleNodeState rns) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState == null) {
            @Nullable DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceId);
            if (deviceProfile != null) {
                deviceState = new DeviceState(ctx, config, deviceId, new ProfileState(deviceProfile), rns);
                deviceStates.put(deviceId, deviceState);
            }
        }
        return deviceState;
    }

    protected void scheduleAlarmHarvesting(@NotNull TbContext ctx, @Nullable TbMsg msg) {
        @NotNull TbMsg periodicCheck = TbMsg.newMsg(PERIODIC_MSG_TYPE, ctx.getTenantId(), msg != null ? msg.getCustomerId() : null, TbMsgMetaData.EMPTY, "{}");
        ctx.tellSelf(periodicCheck, TimeUnit.MINUTES.toMillis(1));
    }

    protected void harvestAlarms(@NotNull TbContext ctx, long ts) throws ExecutionException, InterruptedException {
        for (@NotNull DeviceState state : deviceStates.values()) {
            state.harvestAlarms(ctx, ts);
        }
    }

    protected void updateProfile(@NotNull TbContext ctx, DeviceProfileId deviceProfileId) throws ExecutionException, InterruptedException {
        @Nullable DeviceProfile deviceProfile = cache.get(ctx.getTenantId(), deviceProfileId);
        if (deviceProfile != null) {
            log.debug("[{}] Received device profile update notification: {}", ctx.getSelfId(), deviceProfile);
            for (@NotNull DeviceState state : deviceStates.values()) {
                if (deviceProfile.getId().equals(state.getProfileId())) {
                    state.updateProfile(ctx, deviceProfile);
                }
            }
        } else {
            log.debug("[{}] Received stale profile update notification: [{}]", ctx.getSelfId(), deviceProfileId);
        }
    }

    protected void onProfileUpdate(@NotNull DeviceProfile profile) {
        ctx.tellSelf(TbMsg.newMsg(PROFILE_UPDATE_MSG_TYPE, ctx.getTenantId(), TbMsgMetaData.EMPTY, profile.getId().getId().toString()), 0L);
    }

    private void onDeviceUpdate(@NotNull DeviceId deviceId, @Nullable DeviceProfile deviceProfile) {
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("deviceId", deviceId.getId().toString());
        if (deviceProfile != null) {
            msgData.put("deviceProfileId", deviceProfile.getId().getId().toString());
        }
        ctx.tellSelf(TbMsg.newMsg(DEVICE_UPDATE_MSG_TYPE, ctx.getTenantId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(msgData)), 0L);
    }

    protected void invalidateDeviceProfileCache(DeviceId deviceId, String deviceJson) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            DeviceProfileId currentProfileId = deviceState.getProfileId();
            try {
                @Nullable Device device = JacksonUtil.fromString(deviceJson, Device.class);
                if (!currentProfileId.equals(device.getDeviceProfileId())) {
                    removeDeviceState(deviceId);
                }
            } catch (IllegalArgumentException e) {
                log.debug("[{}] Received device update notification with non-device msg body: [{}][{}]", ctx.getSelfId(), deviceId, e);
            }
        }
    }

    protected void invalidateDeviceProfileCache(DeviceId deviceId, DeviceProfileId deviceProfileId) {
        DeviceState deviceState = deviceStates.get(deviceId);
        if (deviceState != null) {
            if (!deviceState.getProfileId().equals(deviceProfileId)) {
                removeDeviceState(deviceId);
            }
        }
    }

    private void removeDeviceState(DeviceId deviceId) {
        DeviceState state = deviceStates.remove(deviceId);
        if (config.isPersistAlarmRulesState() && (state != null || !config.isFetchAlarmRulesStateOnStart())) {
            ctx.removeRuleNodeStateForEntity(deviceId);
        }
    }
}
