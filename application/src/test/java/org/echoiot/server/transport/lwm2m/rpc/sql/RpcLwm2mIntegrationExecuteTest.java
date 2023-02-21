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


public class RpcLwm2mIntegrationExecuteTest extends AbstractRpcLwM2MIntegrationTest {


    /**
     * Update FW
     * Execute {"id":"5/0/2"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteUpdateFWById_Result_CHANGED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_2;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Update SW
     * Execute {"id":"9/0/4"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteUpdateSWById_Result_CHANGED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_9 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Reboot
     * Execute {"id":"3/0/4"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteRebootById_Result_CHANGED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Registration Update Trigger
     * Execute {"id":"1/0/8"}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteRegistrationUpdateTriggerById_Result_CHANGED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_8;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }


    /**
     * execute_resource_with_parameters (execute reboot after 60 seconds on device)
     * Execute {"id":"3/0/4","value":60}
     * {"result":"CHANGED"}
     */
    @Test
    public void testExecuteResourceWithParametersById_Result_CHANGED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_3 + "/" + RESOURCE_ID_4;
        @NotNull Object expectedValue = 60;
        String actualResult = sendRPCExecuteWithValueById(expectedPath, expectedValue);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CHANGED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * Bootstrap-Request Trigger
     * Execute {"id":"1/0/9"}
     * {"result":"BAD_REQUEST","error":"probably no bootstrap server configured"}
     */
    @Test
    public void testExecuteBootstrapRequestTriggerById_Result_BAD_REQUEST_Error_NoBootstrapServerConfigured() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_1 + "/" + RESOURCE_ID_9;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "probably no bootstrap server configured";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * bad: resource operation not "E"
     * Execute {"id":"5_1.0/0/3"}
     * {"result":"BAD_REQUEST","error":"Resource with /5_1.0/0/3 is not executable."}
     */
    @Test
    public void testExecuteResourceWithOperationNotExecuteById_Result_METHOD_NOT_ALLOWED() throws Exception {
        @NotNull String expectedPath = objectInstanceIdVer_5 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "Resource with " + expectedPath + " is not executable.";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * bad: execute_non_existing_resource_on_non_existing_object
     * Execute {"id":"50/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 50 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testExecuteNonExistingResourceOnNonExistingObjectById_Result_BAD_REQUEST() throws Exception {
        @NotNull String expectedPath = OBJECT_ID_VER_50 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        @NotNull LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        @NotNull String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }

    /**
     * bad: execute security object
     * Execute {"id":"0/0/3"}
     * {"result":"BAD_REQUEST","error":"Specified object id 0 absent in the list supported objects of the client or is security object!"}
     */
    @Test
    public void testExecuteSecurityObjectById_Result_NOT_FOUND() throws Exception {
        @NotNull String expectedPath = objectIdVer_0 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_3;
        String actualResult = sendRPCExecuteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String expectedObjectId = pathIdVerToObjectId(expectedPath);
        @NotNull LwM2mPath expectedPathId = new LwM2mPath(expectedObjectId);
        @NotNull String expected = "Specified object id " + expectedPathId.getObjectId() + " absent in the list supported objects of the client or is security object!";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }


    private String sendRPCExecuteById(String path) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCExecuteWithValueById(String path, Object value) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Execute\", \"params\": {\"id\": \"" + path + "\", \"value\": " + value + " }}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
