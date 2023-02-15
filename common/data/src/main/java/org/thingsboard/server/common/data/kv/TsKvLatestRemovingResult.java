package org.thingsboard.server.common.data.kv;

import lombok.Data;

@Data
public class TsKvLatestRemovingResult {
    private String key;
    private TsKvEntry data;
    private boolean removed;

    public TsKvLatestRemovingResult(TsKvEntry data) {
        this.key = data.getKey();
        this.data = data;
        this.removed = true;
    }

    public TsKvLatestRemovingResult(String key, boolean removed) {
        this.key = key;
        this.removed = removed;
    }
}
