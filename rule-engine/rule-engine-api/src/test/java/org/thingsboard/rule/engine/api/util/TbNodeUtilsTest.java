package org.thingsboard.rule.engine.api.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.common.util.JacksonUtil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TbNodeUtilsTest {

    private static final String DATA_VARIABLE_TEMPLATE = "$[%s]";
    private static final String METADATA_VARIABLE_TEMPLATE = "${%s}";

    @Test
    public void testSimpleReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("metadata_key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("data_key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    @Test
    public void testNoReplacement() {
        String pattern = "ABC ${metadata_key} $[data_key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals(pattern, result);
    }

    @Test
    public void testSameKeysReplacement() {
        String pattern = "ABC ${key} $[key]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode node = JacksonUtil.newObjectNode();
        node.put("key", "data_value");

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value data_value", result);
    }

    @Test
    public void testComplexObjectReplacement() {
        String pattern = "ABC ${key} $[key1.key2.key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value value3", result);
    }

    @Test
    public void testArrayReplacementDoesNotWork() {
        String pattern = "ABC ${key} $[key1.key2[0].key3]";
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("key", "metadata_value");

        ObjectNode key2Node = JacksonUtil.newObjectNode();
        key2Node.put("key3", "value3");

        ObjectNode key1Node = JacksonUtil.newObjectNode();
        key1Node.set("key2", key2Node);


        ObjectNode node = JacksonUtil.newObjectNode();
        node.set("key1", key1Node);

        TbMsg msg = TbMsg.newMsg("CUSTOM", TenantId.SYS_TENANT_ID, md, JacksonUtil.toString(node));
        String result = TbNodeUtils.processPattern(pattern, msg);
        Assert.assertEquals("ABC metadata_value $[key1.key2[0].key3]", result);
    }

    @Test
    public void givenKey_whenFormatDataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is("$[key]"));
        assertThat(TbNodeUtils.formatDataVarTemplate("key"), is(String.format(DATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatDataVarTemplate(""), is("$[]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(""), is(String.format(DATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatDataVarTemplate(null), is("$[null]"));
        assertThat(TbNodeUtils.formatDataVarTemplate(null), is(String.format(DATA_VARIABLE_TEMPLATE, (String) null)));
    }

    @Test
    public void givenKey_whenFormatMetadataVarTemplate_thenReturnTheSameStringAsFormat() {
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is("${key}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate("key"), is(String.format(METADATA_VARIABLE_TEMPLATE, "key")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is("${}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(""), is(String.format(METADATA_VARIABLE_TEMPLATE, "")));

        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is("${null}"));
        assertThat(TbNodeUtils.formatMetadataVarTemplate(null), is(String.format(METADATA_VARIABLE_TEMPLATE, (String) null)));
    }
}
