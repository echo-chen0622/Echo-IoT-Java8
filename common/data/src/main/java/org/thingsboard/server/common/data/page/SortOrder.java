package org.thingsboard.server.common.data.page;

import lombok.Data;

@Data
public class SortOrder {

    private final String property;
    private final Direction direction;

    public SortOrder(String property) {
        this(property, Direction.ASC);
    }

    public SortOrder(String property, Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public static enum Direction {
        ASC, DESC
    }

}
