package org.echoiot.server.dao.relation;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@RequiredArgsConstructor
@Builder
public class RelationCacheKey implements Serializable {

    private static final long serialVersionUID = 3911151843961657570L;

    @NotNull
    private final EntityId from;
    @NotNull
    private final EntityId to;
    @NotNull
    private final String type;
    @NotNull
    private final RelationTypeGroup typeGroup;
    @NotNull
    private final EntitySearchDirection direction;

    public RelationCacheKey(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    @NotNull
    @Override
    public String toString() {
        @NotNull StringBuilder sb = new StringBuilder();
        boolean first = add(sb, true, from);
        first = add(sb, first, to);
        first = add(sb, first, type);
        first = add(sb, first, typeGroup);
        add(sb, first, direction);
        return sb.toString();
    }

    private boolean add(@NotNull StringBuilder sb, boolean first, @Nullable Object param) {
        if (param != null) {
            if (!first) {
                sb.append("_");
            }
            first = false;
            sb.append(param);
        }
        return first;
    }

}
