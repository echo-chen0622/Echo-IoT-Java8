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
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class RpcLwm2mIntegrationReadTest extends AbstractRpcLwM2MIntegrationTest {

    /**
     * Read {"id":"/3"}
     * Read {"id":"/6"}...
     */
    @Test
    public void testReadAllObjectsInClientById_Result_CONTENT_Value_IsLwM2mObject_IsInstances() throws Exception {
                expectedObjectIdVers.forEach(expected -> {
            try {
                String actualResult  = sendRPCById((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                @NotNull LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                @NotNull String expectedObjectInstances = "LwM2mObject [id=" + expectedPath.getObjectId() + ", instances={0=LwM2mObjectInstance [id=0, resources=";
                if (expectedPath.getObjectId() == 2) {
                    expectedObjectInstances = "LwM2mObject [id=2, instances={}]";
                }
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/5/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadAllInstancesInClientById_Result_CONTENT_Value_IsInstances_IsResources() throws Exception{
        expectedObjectIdVerInstances.forEach(expected -> {
            try {
                String actualResult  = sendRPCById((String) expected);
                String expectedObjectId = pathIdVerToObjectId((String) expected);
                @NotNull LwM2mPath expectedPath = new LwM2mPath(expectedObjectId);
                @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
                assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
                @NotNull String expectedObjectInstances = "LwM2mObjectInstance [id=" + expectedPath.getObjectInstanceId() + ", resources={";
                assertTrue(rpcActualResult.get("value").asText().contains(expectedObjectInstances));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Read {"id":"/19/1/0"}
     *
     * @throws Exception
     */
    @Test
    public void testReadMultipleResourceById_Result_CONTENT_Value_IsLwM2mMultipleResource() throws Exception {
       @NotNull String expectedIdVer = objectInstanceIdVer_3 + "/" + RESOURCE_ID_11;
        String actualResult = sendRPCById(expectedIdVer);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "LwM2mMultipleResource [id=" + RESOURCE_ID_11 + ", values={";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"id":"/3/0/14"}
     */
    @Test
    public void testReadSingleResourceById_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
         @NotNull String expectedIdVer = objectInstanceIdVer_3 + "/" + RESOURCE_ID_14;
        String actualResult = sendRPCById(expectedIdVer);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * Read {"key":"UtfOffset"}
     */
    @Test
    public void testReadSingleResourceByKey_Result_CONTENT_Value_IsLwM2mSingleResource() throws Exception {
        @NotNull String expectedKey = RESOURCE_ID_NAME_3_14;
        String actualResult = sendRPCByKey(expectedKey);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected = "LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        assertTrue(rpcActualResult.get("value").asText().contains(expected));
    }

    /**
     * ReadComposite {"ids":["/1_1.2", "/3_1.0/0/1", "/3_1.0/0/11"]}
     */
    @Test
    public void testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectIsLwM2mSingleResourceIsLwM2mMultipleResource() throws Exception {
        @NotNull String expectedIdVer_1 = (String) expectedObjectIdVers.stream().filter(path -> (!((String)path).contains("/" + BINARY_APP_DATA_CONTAINER) && ((String)path).contains("/" + SERVER))).findFirst().get();
        String objectId_1 = pathIdVerToObjectId(expectedIdVer_1);
        @NotNull String expectedIdVer3_0_1 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_1;
        @NotNull String expectedIdVer3_0_11 = objectInstanceIdVer_3 + "/" + RESOURCE_ID_11;
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        @NotNull String expectedIds = "[\"" + expectedIdVer_1 + "\", \"" + expectedIdVer3_0_1 + "\", \"" + expectedIdVer3_0_11 + "\"]";
        String actualResult = sendCompositeRPCByIds(expectedIds);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        @NotNull String expected1 = objectId_1 + "=LwM2mObject [id=" + new LwM2mPath(objectId_1).getObjectId() + ", instances={";
        @NotNull String expected3_0_1 = objectInstanceId_3 + "/" + RESOURCE_ID_1 + "=LwM2mSingleResource [id=" + RESOURCE_ID_1 + ", value=";
        @NotNull String expected3_0_11 = objectInstanceId_3 + "/" + RESOURCE_ID_11 + "=LwM2mMultipleResource [id=" + RESOURCE_ID_11 + ", values={";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected1));
        assertTrue(actualValues.contains(expected3_0_1));
        assertTrue(actualValues.contains(expected3_0_11));
    }

    /**
     * ReadComposite {"ids":["/1_1.2/0/1", "/1_1.2/0/2", "/3_1.0/0"]}
     */
    @Test
    public void testReadCompositeSingleResourceByIds_Result_CONTENT_Value_IsObjectInstanceIsLwM2mSingleResource() throws Exception {
        String expectedIdVer3_0 = objectInstanceIdVer_3;
        @NotNull String expectedIdVer1_0_1 = objectInstanceIdVer_1 + "/" + RESOURCE_ID_1;
        @NotNull String expectedIdVer1_0_2 = objectInstanceIdVer_1 + "/" + RESOURCE_ID_2;
        @NotNull String expectedIds = "[\"" + expectedIdVer1_0_1 + "\", \"" + expectedIdVer1_0_2 + "\", \"" + expectedIdVer3_0 + "\"]";
        String actualResult = sendCompositeRPCByIds(expectedIds);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        @NotNull LwM2mPath path = new LwM2mPath(objectInstanceId_3);
        @NotNull String expected3_0 = objectInstanceId_3 + "=LwM2mObjectInstance [id=" + path.getObjectInstanceId() + ", resources={";
        String objectInstanceId_1 = pathIdVerToObjectId(objectInstanceIdVer_1);
        @NotNull String expected1_0_1 = objectInstanceId_1 + "/" + RESOURCE_ID_1 + "=LwM2mSingleResource [id=" + RESOURCE_ID_1 + ", value=";
        @NotNull String expected1_0_2 = objectInstanceId_1 + "/" + RESOURCE_ID_2 + "=null";
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected3_0));
        assertTrue(actualValues.contains(expected1_0_1));
        assertTrue(actualValues.contains(expected1_0_2));
    }

    /**
     * ReadComposite {"keys":["batteryLevel", "UtfOffset", "dataRead", "dataWrite"]}
     */
    @Test
    public void testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_0_AND_19_0_1__IsLwM2mMultipleResource() throws Exception {
        @NotNull String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        @NotNull String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        @NotNull String expectedKey19_0_0 = RESOURCE_ID_NAME_19_0_0;
        @NotNull String expectedKey19_1_0 = RESOURCE_ID_NAME_19_1_0;
        @NotNull String expectedKey19_X_0 = "=LwM2mMultipleResource [id=0, values={0=LwM2mResourceInstance [id=0, value=1Bytes, type=OPAQUE]";
        @NotNull String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_0 + "\", \"" + expectedKey19_1_0 + "\"]";
        String actualResult = sendCompositeRPCByKeys(expectedKeys);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResult.get("result").asText());
        String objectInstanceId_3 = pathIdVerToObjectId(objectInstanceIdVer_3);
        String objectId_19 = pathIdVerToObjectId(objectIdVer_19);
        @NotNull String expected3_0_9 = objectInstanceId_3 + "/" + RESOURCE_ID_9 + "=LwM2mSingleResource [id=" + RESOURCE_ID_9 + ", value=";
        @NotNull String expected3_0_14 = objectInstanceId_3 + "/" + RESOURCE_ID_14 + "=LwM2mSingleResource [id=" + RESOURCE_ID_14 + ", value=";
        @NotNull String expected19_0_0 = objectId_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0 + expectedKey19_X_0;
        @NotNull String expected19_1_0 = objectId_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + expectedKey19_X_0;
        String actualValues = rpcActualResult.get("value").asText();
        assertTrue(actualValues.contains(expected3_0_9));
        assertTrue(actualValues.contains(expected3_0_14));
        assertTrue(actualValues.contains(expected19_0_0));
        assertTrue(actualValues.contains(expected19_1_0));
    }

    /**
     * ReadComposite {"keys":["batteryLevel", "UtfOffset", "dataDescription"]}
     */
    @Test
    public void testReadCompositeSingleResourceByKeys_Result_CONTENT_Value_3_0_IsLwM2mSingleResource_19_0_3_IsNotConfiguredInTheDeviceProfile() throws Exception {
        @NotNull String expectedKey3_0_9 = RESOURCE_ID_NAME_3_9;
        @NotNull String expectedKey3_0_14 = RESOURCE_ID_NAME_3_14;
        @NotNull String expectedKey19_0_3 = RESOURCE_ID_NAME_19_0_3;
        @NotNull String expectedKeys = "[\"" + expectedKey3_0_9 + "\", \"" + expectedKey3_0_14 + "\", \"" + expectedKey19_0_3 + "\"]";
        String actualResult = sendCompositeRPCByKeys(expectedKeys);
        @Nullable ObjectNode rpcActualResult = JacksonUtil.fromString(actualResult, ObjectNode.class);
        assertEquals(ResponseCode.BAD_REQUEST.getName(), rpcActualResult.get("result").asText());
        String actualValue = rpcActualResult.get("error").asText();
        @NotNull String expectedValue = expectedKey19_0_3 + " is not configured in the device profile!";
        assertEquals(actualValue, expectedValue);
    }


    private String sendRPCById(String path) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"id\": \"" + path + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendRPCByKey(String key) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"Read\", \"params\": {\"key\": \"" + key + "\"}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPCByIds(String paths) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"ids\":" + paths + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }

    private String sendCompositeRPCByKeys(String keys) throws Exception {
        @NotNull String setRpcRequest = "{\"method\": \"ReadComposite\", \"params\": {\"keys\":" + keys + "}}";
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceId, setRpcRequest, String.class, status().isOk());
    }
}
