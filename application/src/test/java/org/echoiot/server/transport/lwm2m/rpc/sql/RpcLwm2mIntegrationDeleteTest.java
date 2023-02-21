package org.echoiot.server.transport.lwm2m.rpc.sql;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.transport.lwm2m.rpc.AbstractRpcLwM2MIntegrationTest;
import org.eclipse.leshan.core.ResponseCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.echoiot.server.transport.lwm2m.Lwm2mTestHelper.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RpcLwm2mIntegrationDeleteTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * if there is such an instance
     * Delete {"id":"/3303/12"}
     * {"result":"DELETE"}
     */
    @Test
    public void testDeleteObjectInstanceIsSuchByIdKey_Result_DELETED() throws Exception {
        @NotNull String expectedPath = objectIdVer_3303 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.DELETED.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * if there is no such instance
     * Delete {"id":"/19/12"}
     * {"result":"NOT_FOUND"}
     */
    @Test
    public void testDeleteObjectInstanceIsNotSuchByIdKey_Result_NOT_FOUND() throws Exception {
        @NotNull String expectedPath = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_12;
        String actualResult = sendRPCDeleteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.NOT_FOUND.getName(), rpcActualResult.get("result").asText());
    }

    /**
     * delete object
     * Delete {"id":"/19_1.1"}
     * {"result":"BAD_REQUEST","error":"Invalid path /19 : Only object instances can be delete"}
     */
    @Test
    public void testDeleteObjectByIdKey_Result_BAD_REQUEST() throws Exception {
        String expectedPath = objectIdVer_19;
        String actualResult = sendRPCDeleteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "Invalid path " + pathIdVerToObjectId(expectedPath) + " : Only object instances can be delete";
        String actual = rpcActualResult.get("error").asText();
        assertEquals(actual, expected);
    }


    /**
     * delete resource
     * Delete {"id":"/3/0/7"}
     * {"result":"METHOD_NOT_ALLOWED"}
     */
    @Test
    public void testDeleteResourceByIdKey_Result_METHOD_NOT_ALLOWED() throws Exception {
        @NotNull String expectedPath = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + RESOURCE_ID_7;
        String actualResult = sendRPCDeleteById(expectedPath);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.METHOD_NOT_ALLOWED.getName(), rpcActualResult.get("result").asText());
    }


    private String sendRPCDeleteById(String path) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Delete\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

}
