package org.thingsboard.server.dao;

import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;

@ContextConfiguration(initializers = RedisSqlTestSuite.class)
@RunWith(ClasspathSuite.class)
@ClassnameFilters(
        //All the same tests using redis instead of caffeine.
        "org.thingsboard.server.dao.service.*ServiceSqlTest"
)
public class RedisSqlTestSuite implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @ClassRule
    public static GenericContainer redis = new GenericContainer("redis:4.0").withExposedPorts(6379);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "cache.type=redis");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.connection.type=standalone");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.host=localhost");
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext, "redis.standalone.port=" + redis.getMappedPort(6379));
    }

}
