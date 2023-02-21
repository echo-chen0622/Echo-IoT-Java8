package org.echoiot.server.common.msg;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Echo on 13.01.18.
 */
@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    @NotNull
    private final Map<String, String> data;

    public TbMsgMetaData() {
        this.data = new ConcurrentHashMap<>();
    }

    public TbMsgMetaData(@NotNull Map<String, String> data) {
        this.data = new ConcurrentHashMap<>();
        data.forEach(this::putValue);
    }

    /**
     * Internal constructor to create immutable TbMsgMetaData.EMPTY
     * */
    private TbMsgMetaData(int ignored) {
        this.data = Collections.emptyMap();
    }

    public String getValue(String key) {
        return this.data.get(key);
    }

    public void putValue(@Nullable String key, @Nullable String value) {
        if (key != null && value != null) {
            this.data.put(key, value);
        }
    }

    @NotNull
    public Map<String, String> values() {
        return new HashMap<>(this.data);
    }

    @NotNull
    public TbMsgMetaData copy() {
        return new TbMsgMetaData(this.data);
    }
}
