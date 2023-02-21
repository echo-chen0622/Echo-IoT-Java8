package org.echoiot.server.dao.sql.query;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class )
@SpringBootTest(classes = EntityKeyMapping.class)
public class EntityKeyMappingTest {

    @Resource
    private EntityKeyMapping entityKeyMapping;

    private static final List<String> result = List.of("device1", "device2", "device3");

    @Test
    public void testSplitToList() {
        @NotNull String value = "device1, device2, device3";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceSingleQuote() {
        @NotNull String value = "'device1', 'device2', 'device3'";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testReplaceDoubleQuote() {
        @NotNull String value = "\"device1\", \"device2\", \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSplitWithoutSpace() {
        @NotNull String value = "\"device1\"    ,    \"device2\"    ,    \"device3\"";
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveSpacesBetweenString() {
        @NotNull String value = "device 1 , device 2  ,         device 3";
        @NotNull List<String> result = List.of("device 1", "device 2", "device 3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testSaveQuoteInString() {
        @NotNull String value = "device ''1 , device \"\"2  ,         device \"'3";
        @NotNull List<String> result = List.of("device ''1", "device \"\"2", "device \"'3");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }

    @Test
    public void testNotDeleteQuoteWhenDifferentStyle() {

        @NotNull String value = "\"device1\", 'device2', \"device3\"";
        @NotNull List<String> result = List.of("\"device1\"", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "'device1', \"device2\", \"device3\"";
        result = List.of("'device1'", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, 'device2', \"device3\"";
        result = List.of("device1", "'device2'", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "'device1', device2, \"device3\"";
        result = List.of("'device1'", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);

        value = "device1, \"device2\", \"device3\"";
        result = List.of("device1", "\"device2\"", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);


        value = "\"device1\", device2, \"device3\"";
        result = List.of("\"device1\"", "device2", "\"device3\"");
        Assert.assertEquals(entityKeyMapping.getListValuesWithoutQuote(value), result);
    }
}
