package org.echoiot.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RpcLwm2mIntegrationCreateTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/12","value":{"0":{"0":"00AC", "1":1}}}
     * {"{"result":"CREATED"}"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdByIdKey_Result_CREATED() throws Exception {
        @NotNull String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        @NotNull String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CREATED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Create  {"id":"/19_1.1","value":{"0":{"0":"00AC"}, "1":1}}
     *
     * create_2_instances_in_object
     * new ObjectInstance if Object is Multiple & Resource Single
     * Create  {"id":"/19_1.1/0","value":{"0":{"0":"00AC", "1":1}}}
     * {"result":"BAD_REQUEST","error":"instance 0 already exists"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAlreadyExistsById_Result_BAD_REQUEST() throws Exception {
        @NotNull String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0;
        @NotNull String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}, \"1\":1}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "instance " + OBJECT_INSTANCE_ID_0 + " already exists";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * failed: cannot_create_mandatory_single_object
     * Create  {"id":"/3/1,"value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Path /3/1. Object must be Multiple !"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdMandatorySingleObjectById_Result_BAD_REQUEST() throws Exception {
        @NotNull String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_1;
        @NotNull String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "Path " + expectedPath + ". Object must be Multiple !";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * failed:  cannot_create_instance_of_security_object
     * Create  {"id":"/0/2","value":{"2":4}}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdSecurityObjectById_Result_BAD_REQUEST() throws Exception {
        @NotNull String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_1;
        @NotNull String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"2\":4}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        @NotNull LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        @NotNull String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * failed: cannot_create_instance_of_absent_object
     * Create  {"id":"/50/1","value":{"0":"00AC"}}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testCreateObjectInstanceWithInstanceIdAbsentObjectById_Result_BAD_REQUEST() throws Exception {
        @NotNull String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_1;
        @NotNull String expectedValue = "{\"" + RESOURCE_ID_0 + "\":{\"0\":\"00AC\"}}";
        String actualResult = sendRPCreateById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        @NotNull LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        @NotNull String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    private String sendRPCreateById(String path, String value) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Create\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
