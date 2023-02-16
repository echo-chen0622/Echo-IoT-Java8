package org.echoiot.server.dao;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.extensions.cpsuite.ClasspathSuite.ClassnameFilters;
import org.junit.runner.RunWith;

@RunWith(ClasspathSuite.class)
@ClassnameFilters({
        "org.echoiot.server.dao.service.*.nosql.*ServiceTimescaleTest",
})
public class TimescaleDaoServiceTestSuite {

}
