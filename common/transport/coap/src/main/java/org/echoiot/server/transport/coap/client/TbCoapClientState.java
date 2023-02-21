package org.echoiot.server.transport.coap.client;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.Device;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.echoiot.server.common.data.device.data.PowerMode;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.coap.TransportConfigurationContainer;
import org.echoiot.server.transport.coap.adaptors.CoapTransportAdaptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class TbCoapClientState {

    private final DeviceId deviceId;
    @NotNull
    private final Lock lock;

    private volatile TransportConfigurationContainer configuration;
    private volatile CoapTransportAdaptor adaptor;
    private volatile ValidateDeviceCredentialsResponse credentials;
    private volatile TransportProtos.SessionInfoProto session;
    private volatile DefaultCoapClientContext.CoapSessionListener listener;
    private volatile TbCoapObservationState attrs;
    private volatile TbCoapObservationState rpc;
    private volatile int contentFormat;

    @Nullable
    private TransportProtos.AttributeUpdateNotificationMsg missedAttributeUpdates;

    private DeviceProfileId profileId;

    @Getter
    private PowerMode powerMode;
    @Getter
    private Long psmActivityTimer;
    @Getter
    private Long edrxCycle;
    @Getter
    private Long pagingTransmissionWindow;
    @Getter
    @Setter
    private boolean asleep;
    @Getter
    private long lastUplinkTime;
    @Getter
    @Setter
    private Future<Void> sleepTask;

    private boolean firstEdrxDownlink = true;

    public TbCoapClientState(DeviceId deviceId) {
        this.deviceId = deviceId;
        this.lock = new ReentrantLock();
    }

    public void init(@NotNull ValidateDeviceCredentialsResponse credentials) {
        this.credentials = credentials;
        this.profileId = credentials.getDeviceInfo().getDeviceProfileId();
        this.powerMode = credentials.getDeviceInfo().getPowerMode();
        this.edrxCycle = credentials.getDeviceInfo().getEdrxCycle();
        this.psmActivityTimer = credentials.getDeviceInfo().getPsmActivityTimer();
        this.pagingTransmissionWindow = credentials.getDeviceInfo().getPagingTransmissionWindow();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public long updateLastUplinkTime(long ts) {
        if (ts > lastUplinkTime) {
            this.lastUplinkTime = ts;
            this.firstEdrxDownlink = true;
        }
        return lastUplinkTime;
    }

    public boolean checkFirstDownlink() {
        boolean result = firstEdrxDownlink;
        firstEdrxDownlink = false;
        return result;
    }

    public void onDeviceUpdate(@NotNull Device device) {
        this.profileId = device.getDeviceProfileId();
        @Nullable var data = device.getDeviceData();
        if (data.getTransportConfiguration() != null && data.getTransportConfiguration().getType().equals(DeviceTransportType.COAP)) {
            CoapDeviceTransportConfiguration configuration = (CoapDeviceTransportConfiguration) data.getTransportConfiguration();
            this.powerMode = configuration.getPowerMode();
            this.edrxCycle = configuration.getEdrxCycle();
            this.psmActivityTimer = configuration.getPsmActivityTimer();
            this.pagingTransmissionWindow = configuration.getPagingTransmissionWindow();
        }
    }

    public void addQueuedNotification(@NotNull TransportProtos.AttributeUpdateNotificationMsg msg) {
        if (missedAttributeUpdates == null) {
            missedAttributeUpdates = msg;
        } else {
            @NotNull Map<String, TransportProtos.TsKvProto> updatedAttrs = new HashMap<>(missedAttributeUpdates.getSharedUpdatedCount() + msg.getSharedUpdatedCount());
            @NotNull Set<String> deletedKeys = new HashSet<>(missedAttributeUpdates.getSharedDeletedCount() + msg.getSharedDeletedCount());
            for (@NotNull TransportProtos.TsKvProto oldUpdatedAttrs : missedAttributeUpdates.getSharedUpdatedList()) {
                updatedAttrs.put(oldUpdatedAttrs.getKv().getKey(), oldUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (@NotNull TransportProtos.TsKvProto newUpdatedAttrs : msg.getSharedUpdatedList()) {
                updatedAttrs.put(newUpdatedAttrs.getKv().getKey(), newUpdatedAttrs);
            }
            deletedKeys.addAll(msg.getSharedDeletedList());
            for (String deletedKey : msg.getSharedDeletedList()) {
                updatedAttrs.remove(deletedKey);
            }
            missedAttributeUpdates = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(updatedAttrs.values()).addAllSharedDeleted(deletedKeys).build();
        }
    }

    public TransportProtos.AttributeUpdateNotificationMsg getAndClearMissedUpdates() {
        var result = this.missedAttributeUpdates;
        this.missedAttributeUpdates = null;
        return result;
    }
}
