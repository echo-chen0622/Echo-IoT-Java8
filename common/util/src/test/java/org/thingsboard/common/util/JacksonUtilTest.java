package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

public class JacksonUtilTest {

    @Test
    public void allow_unquoted_field_mapper_test() {
        String data = "{data: 123}";
        JsonNode actualResult = JacksonUtil.toJsonNode(data, JacksonUtil.ALLOW_UNQUOTED_FIELD_NAMES_MAPPER); // should be: {"data": 123}
        ObjectNode expectedResult = JacksonUtil.newObjectNode();
        expectedResult.put("data", 123); // {"data": 123}
        Assert.assertEquals(expectedResult, actualResult);
        Assert.assertThrows(IllegalArgumentException.class, () -> JacksonUtil.toJsonNode(data)); // syntax exception due to missing quotes in the field name!
    }

}
