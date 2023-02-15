package org.thingsboard.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.util.SqlTsDao;
import org.thingsboard.server.dao.util.TbAutoConfiguration;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sqlts.sql", "org.thingsboard.server.dao.sqlts.insert.sql"})
@EnableJpaRepositories({"org.thingsboard.server.dao.sqlts.ts", "org.thingsboard.server.dao.sqlts.insert.sql"})
@EntityScan({"org.thingsboard.server.dao.model.sqlts.ts"})
@EnableTransactionManagement
@SqlTsDao
public class SqlTsDaoConfig {

}
