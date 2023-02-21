package org.echoiot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.audit.ActionType;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.relation.*;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.dao.relation.RelationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class BaseEntityRelationControllerTest extends AbstractControllerTest {

    public static final String BASE_DEVICE_NAME = "Test dummy device";

    @Resource
    RelationService relationService;

    private IdComparator<EntityView> idComparator;
    private Tenant savedTenant;
    private User tenantAdmin;
    private Device mainDevice;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();
        idComparator = new IdComparator<>();

        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");

        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@echoiot.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        @NotNull Device device = new Device();
        device.setName("Main test device");
        device.setType("default");
        mainDevice = doPost("/api/device", device, Device.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAndFindRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, foundRelation);
    }

    @Test
    public void testSaveWithDeviceFromNotCreated() throws Exception {
        @NotNull Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        @NotNull EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testSaveWithDeviceToNotCreated() throws Exception {
        @NotNull Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testSaveWithDeviceToMissing() throws Exception {
        @NotNull Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        device.setId(new DeviceId(UUID.randomUUID()));
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", device.getId().getId().toString()))));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testSaveAndFindRelationsByFrom() throws Exception {
        final int numOfDevices = 30;

        Mockito.reset(tbClusterService, auditLogService);

        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        @NotNull EntityRelation relationTest = createFromRelation(mainDevice, mainDevice, "TEST_NOTIFY_ENTITY");
        testNotifyEntityAllManyRelation(relationTest, savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, numOfDevices);

        String url = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    public void testSaveAndFindRelationsByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    public void testSaveAndFindRelationsByFromWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        @NotNull String relationType = "TEST";
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }

    @Test
    public void testSaveAndFindRelationsByFromWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        @NotNull String relationType = "TEST";
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        @NotNull String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    public void testSaveAndFindRelationsByToWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        @NotNull String relationType = "TEST";
        @NotNull EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }


    @Test
    public void testSaveAndFindRelationsByToWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        @NotNull String relationType = "TEST";
        @NotNull EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        @NotNull String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    public void testFindRelationsInfoByFrom() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        @Nullable List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByFrom(relationsInfos);
    }

    @Test
    public void testFindRelationsInfoByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        @Nullable List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByTo(relationsInfos);
    }

    @Test
    public void testDeleteRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_DELETED, foundRelation);

        doGet(url).andExpect(status().is4xxClientError());
    }

    @Test
    public void testDeleteRelationWithOtherFromDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                device2.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testDeleteRelationWithOtherToDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device2.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    public void testDeleteRelations() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME + " from");
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME + " to");

        String urlTo = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );
        String urlFrom = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(urlTo, numOfDevices);
        assertFoundList(urlFrom, numOfDevices);

        String url = String.format("/api/relations?entityId=%s&entityType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(null, mainDevice.getId(), mainDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATIONS_DELETED);

        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlTo, List.class).isEmpty()
        );
        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlFrom, List.class).isEmpty()
        );
    }

    @Test
    public void testFindRelationsByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<List<EntityRelation>>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    public void testFindRelationsByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    public void testFindRelationsInfoByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByFrom(relationsInfo);
    }

    @Test
    public void testFindRelationsInfoByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByTo(relationsInfo);
    }

    @Test
    public void testCreateRelationFromTenantToDevice() throws Exception {
        @NotNull EntityRelation relation = new EntityRelation(tenantAdmin.getTenantId(), mainDevice.getId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                tenantAdmin.getTenantId(), EntityType.TENANT,
                "CONTAINS", mainDevice.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    public void testCreateRelationFromDeviceToTenant() throws Exception {
        @NotNull EntityRelation relation = new EntityRelation(mainDevice.getId(), tenantAdmin.getTenantId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", tenantAdmin.getTenantId(), EntityType.TENANT
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    public void testSaveAndFindRelationDifferentTenant() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        loginDifferentTenant();

        doGet(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", relation.getFrom().getId().toString()))));

        deleteDifferentTenant();
    }

    private Device buildSimpleDevice(String name) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device = doPost("/api/device", device, Device.class);
        return device;
    }

    @NotNull
    private EntityRelation createFromRelation(@NotNull Device mainDevice, @NotNull Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }

    private void createDevicesByFrom(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);

            @NotNull EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    private void createDevicesByTo(int numOfDevices, String baseName) throws Exception {
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);
            @NotNull EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    private void assertFoundRelations(@NotNull List<EntityRelation> relations, int numOfDevices) {
        Assert.assertNotNull("Relations is not found!", relations);
        Assert.assertEquals("List of found relations is not equal to number of created relations!",
                numOfDevices, relations.size());
    }

    private void assertFoundList(@NotNull String url, int numOfDevices) throws Exception {
        @SuppressWarnings("unchecked")
        List<EntityRelation> relations = doGet(url, List.class);
        assertFoundRelations(relations, numOfDevices);
    }

    private void assertRelationsInfosByFrom(@NotNull List<EntityRelationInfo> relationsInfos) {
        for (@NotNull EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong FROM entityId!", mainDevice.getId(), info.getFrom());
            Assert.assertTrue("Wrong FROM name!", info.getToName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }

    private void assertRelationsInfosByTo(@NotNull List<EntityRelationInfo> relationsInfos) {
        for (@NotNull EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong TO entityId!", mainDevice.getId(), info.getTo());
            Assert.assertTrue("Wrong TO name!", info.getFromName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }
}
