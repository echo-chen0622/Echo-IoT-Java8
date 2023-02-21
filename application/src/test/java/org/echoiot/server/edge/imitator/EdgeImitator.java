package org.echoiot.server.edge.imitator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.echoiot.edge.rpc.EdgeGrpcClient;
import org.echoiot.edge.rpc.EdgeRpcClient;
import org.echoiot.server.gen.edge.v1.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class EdgeImitator {

    public static final int TIMEOUT_IN_SECONDS = 30;

    private final String routingKey;
    private final String routingSecret;

    private final EdgeRpcClient edgeRpcClient;

    private final Lock lock = new ReentrantLock();

    private CountDownLatch messagesLatch;
    private CountDownLatch responsesLatch;
    private final List<Class<? extends AbstractMessage>> ignoredTypes;

    @Setter
    private boolean randomFailuresOnTimeseriesDownlink = false;
    @Setter
    private double failureProbability = 0.0;

    @Getter
    private EdgeConfiguration configuration;
    @Getter
    private final List<AbstractMessage> downlinkMsgs;

    @Getter
    private UplinkResponseMsg latestResponseMsg;

    private boolean connected = false;

    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        messagesLatch = new CountDownLatch(0);
        responsesLatch = new CountDownLatch(0);
        downlinkMsgs = new ArrayList<>();
        ignoredTypes = new ArrayList<>();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        setEdgeCredentials("rpcHost", host);
        setEdgeCredentials("rpcPort", port);
        setEdgeCredentials("timeoutSecs", 3);
        setEdgeCredentials("keepAliveTimeSec", 300);
    }

    private void setEdgeCredentials(@NotNull String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        @NotNull Field fieldToSet = edgeRpcClient.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(edgeRpcClient, value);
        fieldToSet.setAccessible(false);
    }

    public void connect() {
        connected = true;
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onDownlink,
                this::onClose);

        edgeRpcClient.sendSyncRequestMsg(true);
    }

    public void disconnect() throws InterruptedException {
        connected = false;
        edgeRpcClient.disconnect(false);
    }

    public void sendUplinkMsg(UplinkMsg uplinkMsg) {
        edgeRpcClient.sendUplinkMsg(uplinkMsg);
    }

    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
        latestResponseMsg = msg;
        responsesLatch.countDown();
    }

    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        this.configuration = edgeConfiguration;
    }

    private void onDownlink(@NotNull DownlinkMsg downlinkMsg) {
        @NotNull ListenableFuture<List<Void>> future = processDownlinkMsg(downlinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                @NotNull DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                                                                                      .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                                                                                      .setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                @NotNull DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                                                                                      .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                                                                                      .setSuccess(false).setErrorMsg(t.getMessage()).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    private void onClose(@NotNull Exception e) {
        log.info("onClose: {}", e.getMessage());
    }

    @NotNull
    private ListenableFuture<List<Void>> processDownlinkMsg(@NotNull DownlinkMsg downlinkMsg) {
        @NotNull List<ListenableFuture<Void>> result = new ArrayList<>();
        if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
            for (@NotNull AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                result.add(saveDownlinkMsg(adminSettingsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
            for (@NotNull DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
            for (@NotNull DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
            for (@NotNull DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetProfileUpdateMsgCount() > 0) {
            for (@NotNull AssetProfileUpdateMsg assetProfileUpdateMsg : downlinkMsg.getAssetProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
            for (@NotNull AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
            for (@NotNull RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
            for (@NotNull RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainMetadataUpdateMsg));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
            for (@NotNull DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(saveDownlinkMsg(dashboardUpdateMsg));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
            for (@NotNull RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                result.add(saveDownlinkMsg(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
            for (@NotNull AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(saveDownlinkMsg(alarmUpdateMsg));
            }
        }
        if (downlinkMsg.getEntityDataCount() > 0) {
            for (@NotNull EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                if (randomFailuresOnTimeseriesDownlink) {
                    if (getRandomBoolean()) {
                        result.add(Futures.immediateFailedFuture(new RuntimeException("Random failure. This is expected error for edge test")));
                    } else {
                        result.add(saveDownlinkMsg(entityData));
                    }
                } else {
                    result.add(saveDownlinkMsg(entityData));
                }
            }
        }
        if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
            for (@NotNull EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                result.add(saveDownlinkMsg(entityViewUpdateMsg));
            }
        }
        if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
            for (@NotNull CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                result.add(saveDownlinkMsg(customerUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
            for (@NotNull WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetsBundleUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
            for (@NotNull WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetTypeUpdateMsg));
            }
        }
        if (downlinkMsg.getUserUpdateMsgCount() > 0) {
            for (@NotNull UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                result.add(saveDownlinkMsg(userUpdateMsg));
            }
        }
        if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
            for (@NotNull UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(userCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
            for (@NotNull DeviceRpcCallMsg deviceRpcCallMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                result.add(saveDownlinkMsg(deviceRpcCallMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
            for (@NotNull DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsRequestMsg));
            }
        }
        if (downlinkMsg.getOtaPackageUpdateMsgCount() > 0) {
            for (@NotNull OtaPackageUpdateMsg otaPackageUpdateMsg : downlinkMsg.getOtaPackageUpdateMsgList()) {
                result.add(saveDownlinkMsg(otaPackageUpdateMsg));
            }
        }
        if (downlinkMsg.getQueueUpdateMsgCount() > 0) {
            for (@NotNull QueueUpdateMsg queueUpdateMsg : downlinkMsg.getQueueUpdateMsgList()) {
                result.add(saveDownlinkMsg(queueUpdateMsg));
            }
        }
        if (downlinkMsg.hasEdgeConfiguration()) {
            result.add(saveDownlinkMsg(downlinkMsg.getEdgeConfiguration()));
        }

        return Futures.allAsList(result);
    }

    private boolean getRandomBoolean() {
        double randomValue = ThreadLocalRandom.current().nextDouble() * 100;
        return randomValue <= this.failureProbability;
    }

    @NotNull
    private ListenableFuture<Void> saveDownlinkMsg(@NotNull AbstractMessage message) {
        if (!ignoredTypes.contains(message.getClass())) {
            lock.lock();
            try {
                downlinkMsgs.add(message);
            } finally {
                lock.unlock();
            }
            messagesLatch.countDown();
        }
        return Futures.immediateFuture(null);
    }

    public boolean waitForMessages() throws InterruptedException {
        return waitForMessages(TIMEOUT_IN_SECONDS);
    }

    public boolean waitForMessages(int timeoutInSeconds) throws InterruptedException {
        return messagesLatch.await(timeoutInSeconds, TimeUnit.SECONDS);
    }

    public void expectMessageAmount(int messageAmount) {
        // clear downlinks
        downlinkMsgs.clear();

        messagesLatch = new CountDownLatch(messageAmount);
    }

    public boolean waitForResponses() throws InterruptedException {
        return responsesLatch.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
    }

    public void expectResponsesAmount(int messageAmount) {
        responsesLatch = new CountDownLatch(messageAmount);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> Optional<T> findMessageByType(@NotNull Class<T> tClass) {
        Optional<T> result;
        lock.lock();
        try {
            result = (Optional<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).findAny();
        } finally {
            lock.unlock();
        }
        return result;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> List<T> findAllMessagesByType(@NotNull Class<T> tClass) {
        List<T> result;
        lock.lock();
        try {
            result = (List<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
        return result;
    }

    public AbstractMessage getLatestMessage() {
        return downlinkMsgs.get(downlinkMsgs.size() - 1);
    }

    public void ignoreType(Class<? extends AbstractMessage> type) {
        ignoredTypes.add(type);
    }

    public void allowIgnoredTypes() {
        ignoredTypes.clear();
    }

}
