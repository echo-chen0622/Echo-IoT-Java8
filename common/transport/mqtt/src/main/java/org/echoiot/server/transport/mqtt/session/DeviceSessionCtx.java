package org.echoiot.server.transport.mqtt.session;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.TransportPayloadType;
import org.echoiot.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.echoiot.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.echoiot.server.gen.transport.TransportProtos;
import org.echoiot.server.transport.mqtt.MqttTransportContext;
import org.echoiot.server.transport.mqtt.TopicType;
import org.echoiot.server.transport.mqtt.adaptors.BackwardCompatibilityAdaptor;
import org.echoiot.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.echoiot.server.transport.mqtt.util.MqttTopicFilter;
import org.echoiot.server.transport.mqtt.util.MqttTopicFilterFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class DeviceSessionCtx extends MqttDeviceAwareSessionContext {

    @Getter
    @Setter
    private ChannelHandlerContext channel;

    @Getter
    private final MqttTransportContext context;

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<MqttMessage> msgQueue = new ConcurrentLinkedQueue<>();

    @Getter
    private final Lock msgQueueProcessorLock = new ReentrantLock();

    private final AtomicInteger msgQueueSize = new AtomicInteger(0);

    @Getter
    @Setter
    private boolean provisionOnly = false;

    private volatile MqttTopicFilter telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
    private volatile MqttTopicFilter attributesTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
    private volatile TransportPayloadType payloadType = TransportPayloadType.JSON;
    @Nullable
    private volatile Descriptors.Descriptor attributesDynamicMessageDescriptor;
    @Nullable
    private volatile Descriptors.Descriptor telemetryDynamicMessageDescriptor;
    @Nullable
    private volatile Descriptors.Descriptor rpcResponseDynamicMessageDescriptor;
    private volatile DynamicMessage.Builder rpcRequestDynamicMessageBuilder;
    private volatile MqttTransportAdaptor adaptor;
    private volatile boolean jsonPayloadFormatCompatibilityEnabled;
    private volatile boolean useJsonPayloadFormatForDefaultDownlinkTopics;
    private volatile boolean sendAckOnValidationException;

    @Getter
    @Setter
    private TransportPayloadType provisionPayloadType = payloadType;

    public DeviceSessionCtx(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap, MqttTransportContext context) {
        super(sessionId, mqttQoSMap);
        this.context = context;
        this.adaptor = context.getJsonMqttAdaptor();
    }

    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    public boolean isDeviceTelemetryTopic(String topicName) {
        return telemetryTopicFilter.filter(topicName);
    }

    public boolean isDeviceAttributesTopic(String topicName) {
        return attributesTopicFilter.filter(topicName);
    }

    public MqttTransportAdaptor getPayloadAdaptor() {
        return adaptor;
    }

    public boolean isJsonPayloadType() {
        return payloadType.equals(TransportPayloadType.JSON);
    }

    public boolean isSendAckOnValidationException() {
        return sendAckOnValidationException;
    }

    public Descriptors.Descriptor getTelemetryDynamicMsgDescriptor() {
        return telemetryDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor() {
        return attributesDynamicMessageDescriptor;
    }

    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor() {
        return rpcResponseDynamicMessageDescriptor;
    }

    public DynamicMessage.Builder getRpcRequestDynamicMessageBuilder() {
        return rpcRequestDynamicMessageBuilder;
    }

    @Override
    public void setDeviceProfile(DeviceProfile deviceProfile) {
        super.setDeviceProfile(deviceProfile);
        updateDeviceSessionConfiguration(deviceProfile);
    }

    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(sessionInfo, deviceProfile);
        updateDeviceSessionConfiguration(deviceProfile);
    }

    private void updateDeviceSessionConfiguration(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.MQTT) &&
            transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttConfig = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttConfig.getTransportPayloadTypeConfiguration();
            payloadType = transportPayloadTypeConfiguration.getTransportPayloadType();
            telemetryTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceTelemetryTopic());
            attributesTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceAttributesTopic());
            sendAckOnValidationException = mqttConfig.isSendAckOnValidationException();
            if (TransportPayloadType.PROTOBUF.equals(payloadType)) {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfig = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                updateDynamicMessageDescriptors(protoTransportPayloadConfig);
                jsonPayloadFormatCompatibilityEnabled = protoTransportPayloadConfig.isEnableCompatibilityWithJsonPayloadFormat();
                useJsonPayloadFormatForDefaultDownlinkTopics = jsonPayloadFormatCompatibilityEnabled && protoTransportPayloadConfig.isUseJsonPayloadFormatForDefaultDownlinkTopics();
            }
        } else {
            telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
            attributesTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
            payloadType = TransportPayloadType.JSON;
            sendAckOnValidationException = false;
        }
        updateAdaptor();
    }

    private void updateDynamicMessageDescriptors(ProtoTransportPayloadConfiguration protoTransportPayloadConfig) {
        telemetryDynamicMessageDescriptor = protoTransportPayloadConfig.getTelemetryDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceTelemetryProtoSchema());
        attributesDynamicMessageDescriptor = protoTransportPayloadConfig.getAttributesDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceAttributesProtoSchema());
        rpcResponseDynamicMessageDescriptor = protoTransportPayloadConfig.getRpcResponseDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceRpcResponseProtoSchema());
        rpcRequestDynamicMessageBuilder = protoTransportPayloadConfig.getRpcRequestDynamicMessageBuilder(protoTransportPayloadConfig.getDeviceRpcRequestProtoSchema());
    }

    public MqttTransportAdaptor getAdaptor(TopicType topicType) {
        switch (topicType) {
            case V2:
                return getDefaultAdaptor();
            case V2_JSON:
                return context.getJsonMqttAdaptor();
            case V2_PROTO:
                return context.getProtoMqttAdaptor();
            default:
                return useJsonPayloadFormatForDefaultDownlinkTopics ? context.getJsonMqttAdaptor() : getDefaultAdaptor();
        }
    }

    private MqttTransportAdaptor getDefaultAdaptor() {
        return isJsonPayloadType() ? context.getJsonMqttAdaptor() : context.getProtoMqttAdaptor();
    }

    private void updateAdaptor() {
        if (isJsonPayloadType()) {
            adaptor = context.getJsonMqttAdaptor();
            jsonPayloadFormatCompatibilityEnabled = false;
            useJsonPayloadFormatForDefaultDownlinkTopics = false;
        } else {
            if (jsonPayloadFormatCompatibilityEnabled) {
                adaptor = new BackwardCompatibilityAdaptor(context.getProtoMqttAdaptor(), context.getJsonMqttAdaptor());
            } else {
                adaptor = context.getProtoMqttAdaptor();
            }
        }
    }

    public void addToQueue(MqttMessage msg) {
        msgQueueSize.incrementAndGet();
        ReferenceCountUtil.retain(msg);
        msgQueue.add(msg);
    }

    public void tryProcessQueuedMsgs(Consumer<MqttMessage> msgProcessor) {
        while (!msgQueue.isEmpty()) {
            if (msgQueueProcessorLock.tryLock()) {
                try {
                    MqttMessage msg;
                    while ((msg = msgQueue.poll()) != null) {
                        try {
                            msgQueueSize.decrementAndGet();
                            msgProcessor.accept(msg);
                        } finally {
                            ReferenceCountUtil.safeRelease(msg);
                        }
                    }
                } finally {
                    msgQueueProcessorLock.unlock();
                }
            } else {
                return;
            }
        }
    }

    public int getMsgQueueSize() {
        return msgQueueSize.get();
    }

    public void release() {
        if (!msgQueue.isEmpty()) {
            log.warn("doDisconnect for device {} but unprocessed messages {} left in the msg queue", getDeviceId(), msgQueue.size());
            msgQueue.forEach(ReferenceCountUtil::safeRelease);
            msgQueue.clear();
        }
    }

    public Collection<MqttMessage> getMsgQueueSnapshot(){
        return Collections.unmodifiableCollection(msgQueue);
    }

}
