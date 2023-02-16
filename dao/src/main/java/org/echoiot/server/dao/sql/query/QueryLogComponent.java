package org.echoiot.server.dao.sql.query;

public interface QueryLogComponent {

    void logQuery(QueryContext ctx, String query, long duration);
}
