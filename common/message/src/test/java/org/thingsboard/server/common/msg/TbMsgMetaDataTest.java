package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TbMsgMetaDataTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String metadataJsonStr = "{\"deviceName\":\"Test Device\",\"deviceType\":\"default\",\"ts\":\"1645112691407\"}";
    private JsonNode metadataJson;
    private Map<String, String> metadataExpected;

    @Before
    public void startInit() throws Exception {
        metadataJson = objectMapper.readValue(metadataJsonStr, JsonNode.class);
        metadataExpected = objectMapper.convertValue(metadataJson, new TypeReference<>() {
        });
    }

    @Test
    public void testScript_whenMetadataWithoutPropertiesValueNull_returnMetadataWithAllValue() {
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.values();
        assertEquals(metadataExpected.size(), dataActual.size());
    }

    @Test
    public void testScript_whenMetadataWithPropertiesValueNull_returnMetadataWithoutPropertiesValueEqualsNull() {
        metadataExpected.put("deviceName", null);
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.copy().getData();
        assertEquals(metadataExpected.size() - 1, dataActual.size());
    }
}
