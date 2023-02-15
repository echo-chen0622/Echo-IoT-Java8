package org.thingsboard.server.dao.sql.event;

public interface EventCleanupRepository {

    void cleanupEvents(long eventExpTime, boolean debug);

    void migrateEvents(long regularEventTs, long debugEventTs);
}
