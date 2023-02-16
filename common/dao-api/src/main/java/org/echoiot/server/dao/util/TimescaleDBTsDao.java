package org.echoiot.server.dao.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "database.ts", value = "type", havingValue = "timescale")
public @interface TimescaleDBTsDao {
}