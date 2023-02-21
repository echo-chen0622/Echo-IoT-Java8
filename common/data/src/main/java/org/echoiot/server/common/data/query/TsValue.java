package org.echoiot.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TsValue {

    public static final TsValue EMPTY = new TsValue(0, "");

    private final long ts;
    @NotNull
    private final String value;
    @NotNull
    private final Long count;

    public TsValue(long ts, String value) {
        this(ts, value, null);
    }

}
