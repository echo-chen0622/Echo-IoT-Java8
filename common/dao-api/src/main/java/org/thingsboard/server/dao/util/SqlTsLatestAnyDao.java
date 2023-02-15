package org.thingsboard.server.dao.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnExpression("'${database.ts_latest.type}'=='sql' || '${database.ts_latest.type}'=='timescale'")
public @interface SqlTsLatestAnyDao {
}
