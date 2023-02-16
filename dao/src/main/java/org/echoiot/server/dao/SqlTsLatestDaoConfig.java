package org.echoiot.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.echoiot.server.dao.util.SqlTsLatestDao;
import org.echoiot.server.dao.util.TbAutoConfiguration;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sqlts.sql"})
@EnableJpaRepositories({"org.thingsboard.server.dao.sqlts.insert.latest.sql", "org.thingsboard.server.dao.sqlts.latest"})
@EntityScan({"org.thingsboard.server.dao.model.sqlts.latest"})
@EnableTransactionManagement
@SqlTsLatestDao
public class SqlTsLatestDaoConfig {

}
