package org.echoiot.rule.engine.math;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TbMathArgumentValueTest {

    @Test
    public void test_fromMessageBody_then_defaultValue() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        tbMathArgument.setDefaultValue(5.0);
        @NotNull TbMathArgumentValue result = TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.ofNullable(JacksonUtil.newObjectNode()));
        Assert.assertEquals(5.0, result.getValue(), 0d);
    }

    @Test
    public void test_fromMessageBody_then_emptyBody() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        Throwable thrown = assertThrows(RuntimeException.class, () -> {
            @NotNull TbMathArgumentValue result = TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.empty());
        });
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromMessageBody_then_noKey() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.ofNullable(JacksonUtil.newObjectNode())));
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromMessageBody_then_valueEmpty() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.putNull("TestKey");

        //null value
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.of(msgData)));
        Assert.assertNotNull(thrown.getMessage());

        //empty value
        msgData.put("TestKey", "");
        thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.of(msgData)));
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromMessageBody_then_valueCantConvert_to_double() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        ObjectNode msgData = JacksonUtil.newObjectNode();
        msgData.put("TestKey", "Test");

        //string value
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.of(msgData)));
        Assert.assertNotNull(thrown.getMessage());

        //object value
        msgData.set("TestKey", JacksonUtil.newObjectNode());
        thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageBody(tbMathArgument, Optional.of(msgData)));
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromMessageMetadata_then_noKey() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageMetadata(tbMathArgument, new TbMsgMetaData()));
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromMessageMetadata_then_valueEmpty() {
        @NotNull TbMathArgument tbMathArgument = new TbMathArgument(TbMathArgumentType.MESSAGE_BODY, "TestKey");
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromMessageMetadata(tbMathArgument, null));
        Assert.assertNotNull(thrown.getMessage());
    }

    @Test
    public void test_fromString_thenOK() {
        @NotNull var value = "5.0";
        @NotNull TbMathArgumentValue result = TbMathArgumentValue.fromString(value);
        Assert.assertNotNull(result);
        Assert.assertEquals(5.0, result.getValue(), 0d);
    }

    @Test
    public void test_fromString_then_failure() {
        @NotNull var value = "Test";
        Throwable thrown = assertThrows(RuntimeException.class, () -> TbMathArgumentValue.fromString(value));
        Assert.assertNotNull(thrown.getMessage());
    }
}
