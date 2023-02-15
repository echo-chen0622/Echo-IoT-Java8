package org.thingsboard.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.thingsboard.server.dao.util.TbAutoConfiguration;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.thingsboard.server.dao.sqlts.timescale"})
@EnableJpaRepositories({"org.thingsboard.server.dao.sqlts.timescale", "org.thingsboard.server.dao.sqlts.insert.timescale"})
@EntityScan({"org.thingsboard.server.dao.model.sqlts.timescale"})
@EnableTransactionManagement
@TimescaleDBTsDao
public class TimescaleDaoConfig {

}
