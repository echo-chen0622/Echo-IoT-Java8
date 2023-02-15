package org.thingsboard.server.transport.mqtt.session;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilter;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilterFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 30.08.18.
 */
public abstract class MqttDeviceAwareSessionContext extends DeviceAwareSessionContext {

    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    public MqttDeviceAwareSessionContext(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(sessionId);
        this.mqttQoSMap = mqttQoSMap;
    }

    public ConcurrentMap<MqttTopicMatcher, Integer> getMqttQoSMap() {
        return mqttQoSMap;
    }

    public MqttQoS getQoSForTopic(String topic) {
        List<Integer> qosList = mqttQoSMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(topic))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (!qosList.isEmpty()) {
            return MqttQoS.valueOf(qosList.get(0));
        } else {
            return MqttQoS.AT_LEAST_ONCE;
        }
    }
}
