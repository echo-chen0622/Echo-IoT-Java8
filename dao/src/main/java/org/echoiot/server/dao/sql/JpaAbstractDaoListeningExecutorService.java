package org.echoiot.server.dao.sql;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

@Slf4j
public abstract class JpaAbstractDaoListeningExecutorService {

    @Resource
    protected JpaExecutorService service;

    @Resource
    protected DataSource dataSource;

    @Resource
    protected JdbcTemplate jdbcTemplate;

    protected void printWarnings(@NotNull Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.debug("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.debug("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

}
