package org.thingsboard.server.cache;

import lombok.Data;

@Data
public class CacheSpecs {
    private Integer timeToLiveInMinutes;
    private Integer maxSize;
}
