package org.echoiot.server.dao;

import org.echoiot.server.dao.util.SqlTsDao;
import org.echoiot.server.dao.util.TbAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.echoiot.server.dao.sqlts.sql", "org.echoiot.server.dao.sqlts.insert.sql"})
@EnableJpaRepositories({"org.echoiot.server.dao.sqlts.ts", "org.echoiot.server.dao.sqlts.insert.sql"})
@EntityScan({"org.echoiot.server.dao.model.sqlts.ts"})
@EnableTransactionManagement
@SqlTsDao
public class SqlTsDaoConfig {

}
