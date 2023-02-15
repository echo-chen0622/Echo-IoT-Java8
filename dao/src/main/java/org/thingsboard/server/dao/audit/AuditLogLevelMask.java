package org.thingsboard.server.dao.audit;

import lombok.Getter;

@Getter
public enum AuditLogLevelMask {

    OFF(false, false),
    W(true, false),
    RW(true, true);

    private final boolean write;
    private final boolean read;

    AuditLogLevelMask(boolean write, boolean read) {
        this.write = write;
        this.read = read;
    }
}
