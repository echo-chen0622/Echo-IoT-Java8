package org.thingsboard.server.dao.sqlts;

import lombok.Data;

import java.util.UUID;

@Data
public class TsKey {
    private final UUID entityId;
    private final int key;
}
