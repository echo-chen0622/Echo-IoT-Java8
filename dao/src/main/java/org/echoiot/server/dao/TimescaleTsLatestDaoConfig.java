package org.echoiot.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.echoiot.server.dao.util.TbAutoConfiguration;
import org.echoiot.server.dao.util.TimescaleDBTsLatestDao;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.echoiot.server.dao.sqlts.timescale"})
@EnableJpaRepositories({"org.echoiot.server.dao.sqlts.insert.latest.sql", "org.echoiot.server.dao.sqlts.latest"})
@EntityScan({"org.echoiot.server.dao.model.sqlts.latest"})
@EnableTransactionManagement
@TimescaleDBTsLatestDao
public class TimescaleTsLatestDaoConfig {

}
