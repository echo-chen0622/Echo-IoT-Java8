package org.echoiot.server.common.data.kv;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class TsKvLatestRemovingResult {
    private String key;
    private TsKvEntry data;
    private boolean removed;

    public TsKvLatestRemovingResult(@NotNull TsKvEntry data) {
        this.key = data.getKey();
        this.data = data;
        this.removed = true;
    }

    public TsKvLatestRemovingResult(String key, boolean removed) {
        this.key = key;
        this.removed = removed;
    }
}
