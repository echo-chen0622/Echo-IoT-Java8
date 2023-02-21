package org.echoiot.server.dao.service;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@TestPropertySource(locations = {"classpath:cassandra-test.properties", "classpath:application-test.properties", "classpath:nosql-test.properties"})
public @interface DaoNoSqlTest {
}
