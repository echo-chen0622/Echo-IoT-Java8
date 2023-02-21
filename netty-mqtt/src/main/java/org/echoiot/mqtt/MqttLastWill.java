package org.echoiot.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"WeakerAccess", "unused", "SimplifiableIfStatement", "StringBufferReplaceableByString"})
public final class MqttLastWill {

    @NotNull
    private final String topic;
    @NotNull
    private final String message;
    private final boolean retain;
    @NotNull
    private final MqttQoS qos;

    public MqttLastWill(@NotNull String topic, @NotNull String message, boolean retain, @NotNull MqttQoS qos) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        if(message == null){
            throw new NullPointerException("message");
        }
        if(qos == null){
            throw new NullPointerException("qos");
        }
        this.topic = topic;
        this.message = message;
        this.retain = retain;
        this.qos = qos;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    @NotNull
    public String getMessage() {
        return message;
    }

    public boolean isRetain() {
        return retain;
    }

    @NotNull
    public MqttQoS getQos() {
        return qos;
    }

    @NotNull
    public static MqttLastWill.Builder builder(){
        return new MqttLastWill.Builder();
    }

    public static final class Builder {

        private String topic;
        private String message;
        private boolean retain;
        private MqttQoS qos;

        public String getTopic() {
            return topic;
        }

        @NotNull
        public Builder setTopic(@NotNull String topic) {
            if(topic == null){
                throw new NullPointerException("topic");
            }
            this.topic = topic;
            return this;
        }

        public String getMessage() {
            return message;
        }

        @NotNull
        public Builder setMessage(@NotNull String message) {
            if(message == null){
                throw new NullPointerException("message");
            }
            this.message = message;
            return this;
        }

        public boolean isRetain() {
            return retain;
        }

        @NotNull
        public Builder setRetain(boolean retain) {
            this.retain = retain;
            return this;
        }

        public MqttQoS getQos() {
            return qos;
        }

        @NotNull
        public Builder setQos(@NotNull MqttQoS qos) {
            if(qos == null){
                throw new NullPointerException("qos");
            }
            this.qos = qos;
            return this;
        }

        @NotNull
        public MqttLastWill build(){
            return new MqttLastWill(topic, message, retain, qos);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        @NotNull MqttLastWill that = (MqttLastWill) o;

        if (retain != that.retain) return false;
        if (!topic.equals(that.topic)) return false;
        if (!message.equals(that.message)) return false;
        return qos == that.qos;

    }

    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (retain ? 1 : 0);
        result = 31 * result + qos.hashCode();
        return result;
    }

    @NotNull
    @Override
    public String toString() {
        @NotNull final StringBuilder sb = new StringBuilder("MqttLastWill{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", retain=").append(retain);
        sb.append(", qos=").append(qos.name());
        sb.append('}');
        return sb.toString();
    }
}
