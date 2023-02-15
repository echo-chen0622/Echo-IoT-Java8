package org.thingsboard.server.common.msg;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    private final Map<String, String> data;

    public TbMsgMetaData() {
        this.data = new ConcurrentHashMap<>();
    }

    public TbMsgMetaData(Map<String, String> data) {
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

    public void putValue(String key, String value) {
        if (key != null && value != null) {
            this.data.put(key, value);
        }
    }

    public Map<String, String> values() {
        return new HashMap<>(this.data);
    }

    public TbMsgMetaData copy() {
        return new TbMsgMetaData(this.data);
    }
}
