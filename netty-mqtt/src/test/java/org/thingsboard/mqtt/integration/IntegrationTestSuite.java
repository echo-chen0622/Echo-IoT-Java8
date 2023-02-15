package org.thingsboard.mqtt.integration;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
        "org.thingsboard.mqtt.integration.*Test",
})
public class IntegrationTestSuite {

}
