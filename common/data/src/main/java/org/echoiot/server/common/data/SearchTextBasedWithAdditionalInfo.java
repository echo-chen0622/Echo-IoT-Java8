package org.echoiot.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.id.UUIDBased;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.Consumer;

/**
 * Created by Echo on 19.02.18.
 */
@Slf4j
public abstract class SearchTextBasedWithAdditionalInfo<I extends UUIDBased> extends SearchTextBased<I> implements HasAdditionalInfo {

    public static final ObjectMapper mapper = new ObjectMapper();
    @NoXss
    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    public SearchTextBasedWithAdditionalInfo() {
        super();
    }

    public SearchTextBasedWithAdditionalInfo(I id) {
        super(id);
    }

    public SearchTextBasedWithAdditionalInfo(@NotNull SearchTextBasedWithAdditionalInfo<I> searchTextBased) {
        super(searchTextBased);
        setAdditionalInfo(searchTextBased.getAdditionalInfo());
    }

    @Nullable
    @Override
    public JsonNode getAdditionalInfo() {
        return getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode addInfo) {
        setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        @NotNull SearchTextBasedWithAdditionalInfo<?> that = (SearchTextBasedWithAdditionalInfo<?>) o;
        return Arrays.equals(additionalInfoBytes, that.additionalInfoBytes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), additionalInfoBytes);
    }

    @Nullable
    public static JsonNode getJson(@NotNull Supplier<JsonNode> jsonData, @NotNull Supplier<byte[]> binaryData) {
        JsonNode json = jsonData.get();
        if (json != null) {
            return json;
        } else {
            byte[] data = binaryData.get();
            if (data != null) {
                try {
                    return mapper.readTree(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    log.warn("Can't deserialize json data: ", e);
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static void setJson(JsonNode json, @NotNull Consumer<JsonNode> jsonConsumer, @NotNull Consumer<byte[]> bytesConsumer) {
        jsonConsumer.accept(json);
        try {
            bytesConsumer.accept(mapper.writeValueAsBytes(json));
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize json data: ", e);
        }
    }
}
