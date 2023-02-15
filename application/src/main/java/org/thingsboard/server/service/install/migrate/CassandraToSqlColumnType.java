package org.thingsboard.server.service.install.migrate;

public enum CassandraToSqlColumnType {
    ID,
    DOUBLE,
    INTEGER,
    FLOAT,
    BIGINT,
    BOOLEAN,
    STRING,
    JSON,
    ENUM_TO_INT
}
