package org.echoiot.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Data
public class BranchInfo {
    private final String name;

    @JsonProperty("default")
    private final boolean isDefault;

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchInfo that = (BranchInfo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
