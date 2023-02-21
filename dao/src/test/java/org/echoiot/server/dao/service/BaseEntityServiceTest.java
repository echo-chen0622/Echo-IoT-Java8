package org.echoiot.server.dao.service;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.asset.Asset;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.kv.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.query.*;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.EntitySearchDirection;
import org.echoiot.server.common.data.relation.RelationEntityTypeFilter;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.dao.attributes.AttributesService;
import org.echoiot.server.dao.model.sqlts.ts.TsKvEntity;
import org.echoiot.server.dao.sql.relation.RelationRepository;
import org.echoiot.server.dao.timeseries.TimeseriesService;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
public abstract class BaseEntityServiceTest extends AbstractServiceTest {

    static final int ENTITY_COUNT = 5;

    @Resource
    private AttributesService attributesService;

    @Resource
    private TimeseriesService timeseriesService;

    private TenantId tenantId;

    @Resource
    private JdbcTemplate template;

    @Resource
    private RelationRepository relationRepository;

    @Before
    public void before() {
        @NotNull Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }


    @Test
    public void testCountEntitiesByQuery() throws InterruptedException {
        @NotNull List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
        }

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setDeviceType("unknown");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setDeviceType("default");
        filter.setDeviceNameFilter("Device1");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(11, count);

        @NotNull EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.DEVICE);
        entityListFilter.setEntityList(devices.stream().map(Device::getId).map(DeviceId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        deviceService.deleteDevicesByTenantId(tenantId);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }


    @Test
    public void testCountHierarchicalEntitiesByQuery() throws InterruptedException {
        @NotNull List<Asset> assets = new ArrayList<>();
        @NotNull List<Device> devices = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        @NotNull RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);

        @NotNull EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(31, count); //due to the loop relations in hierarchy, the TenantId included in total count (1*Tenant + 5*Asset + 5*5*Devices = 31)

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(25, count);

        filter.setRootEntity(devices.get(0).getId());
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Manages", Collections.singletonList(EntityType.TENANT))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(1, count);

        @NotNull DeviceSearchQueryFilter filter2 = new DeviceSearchQueryFilter();
        filter2.setRootEntity(tenantId);
        filter2.setDirection(EntitySearchDirection.FROM);
        filter2.setRelationType("Contains");

        countQuery = new EntityCountQuery(filter2);

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(25, count);

        filter2.setDeviceTypes(Arrays.asList("default0", "default1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(10, count);

        filter2.setRootEntity(devices.get(0).getId());
        filter2.setDirection(EntitySearchDirection.TO);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        @NotNull AssetSearchQueryFilter filter3 = new AssetSearchQueryFilter();
        filter3.setRootEntity(tenantId);
        filter3.setDirection(EntitySearchDirection.FROM);
        filter3.setRelationType("Manages");

        countQuery = new EntityCountQuery(filter3);

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(5, count);

        filter3.setAssetTypes(Arrays.asList("type0", "type1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(2, count);

        filter3.setRootEntity(devices.get(0).getId());
        filter3.setDirection(EntitySearchDirection.TO);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testCountEdgeEntitiesByQuery() throws InterruptedException {
        @NotNull List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            @NotNull Edge edge = createEdge(i, "default");
            edges.add(edgeService.saveEdge(edge));
        }

        @NotNull EdgeTypeFilter filter = new EdgeTypeFilter();
        filter.setEdgeType("default");
        filter.setEdgeNameFilter("");

        @NotNull EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        filter.setEdgeType("unknown");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);

        filter.setEdgeType("default");
        filter.setEdgeNameFilter("Edge1");
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(11, count);

        @NotNull EntityListFilter entityListFilter = new EntityListFilter();
        entityListFilter.setEntityType(EntityType.EDGE);
        entityListFilter.setEntityList(edges.stream().map(Edge::getId).map(EdgeId::toString).collect(Collectors.toList()));

        countQuery = new EntityCountQuery(entityListFilter);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(97, count);

        edgeService.deleteEdgesByTenantId(tenantId);
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(0, count);
    }

    @Test
    public void testCountHierarchicalEntitiesByEdgeSearchQuery() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Edge edge = createEdge(i, "type" + i);
            edge = edgeService.saveEdge(edge);
            //TO make sure devices have different created time
            Thread.sleep(1);

            @NotNull EntityRelation er = new EntityRelation();
            er.setFrom(tenantId);
            er.setTo(edge.getId());
            er.setType("Manages");
            er.setTypeGroup(RelationTypeGroup.COMMON);
            relationService.saveRelation(tenantId, er);
        }

        @NotNull EdgeSearchQueryFilter filter = new EdgeSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        @NotNull EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(5, count);

        filter.setEdgeTypes(Arrays.asList("type0", "type1"));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(2, count);
    }

    @NotNull
    private Edge createEdge(int i, String type) {
        @NotNull Edge edge = new Edge();
        edge.setTenantId(tenantId);
        edge.setName("Edge" + i);
        edge.setType(type);
        edge.setLabel("EdgeLabel" + i);
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        return edge;
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQuery() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(0, false);
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQueryWithLevel() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(2, false);
    }

    @Test
    public void testHierarchicalFindEntityDataWithAttributesByQueryWithLastLevelOnly() throws ExecutionException, InterruptedException {
        doTestHierarchicalFindEntityDataWithAttributesByQuery(2, true);
    }

    private void doTestHierarchicalFindEntityDataWithAttributesByQuery(final int maxLevel, final boolean fetchLastLevelOnly) throws ExecutionException, InterruptedException {
        @NotNull List<Asset> assets = new ArrayList<>();
        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Long> temperatures = new ArrayList<>();
        @NotNull List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("Contains", Collections.singletonList(EntityType.DEVICE))));
        filter.setMaxLevel(maxLevel);
        filter.setFetchLastLevelOnly(fetchLastLevelOnly);

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(25, loadedEntities.size());
        @NotNull List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        @NotNull List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        @NotNull List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testCountHierarchicalEntitiesByMultiRootQuery() throws InterruptedException {
        @NotNull List<Asset> buildings = new ArrayList<>();
        @NotNull List<Asset> apartments = new ArrayList<>();
        @NotNull Map<String, Map<UUID, String>> entityNameByTypeMap = new HashMap<>();
        @NotNull Map<UUID, UUID> childParentRelationMap = new HashMap<>();
        createMultiRootHierarchy(buildings, apartments, entityNameByTypeMap, childParentRelationMap);

        @NotNull RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setMultiRoot(true);
        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(buildings.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.FROM);

        @NotNull EntityCountQuery countQuery = new EntityCountQuery(filter);

        long count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(63, count);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(27, count);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));

        count = entityService.countEntitiesByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), countQuery);
        Assert.assertEquals(9, count);

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);

    }

    @Test
    public void testMultiRootHierarchicalFindEntityDataWithAttributesByQuery() throws ExecutionException, InterruptedException {
        @NotNull List<Asset> buildings = new ArrayList<>();
        @NotNull List<Asset> apartments = new ArrayList<>();
        @NotNull Map<String, Map<UUID, String>> entityNameByTypeMap = new HashMap<>();
        @NotNull Map<UUID, UUID> childParentRelationMap = new HashMap<>();
        createMultiRootHierarchy(buildings, apartments, entityNameByTypeMap, childParentRelationMap);

        @NotNull RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setMultiRoot(true);
        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(buildings.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.FROM);

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Lists.newArrayList(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "parentId"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "type")
                                                                  );
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "status"));

        @NotNull KeyFilter onlineStatusFilter = new KeyFilter();
        onlineStatusFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setOperation(StringFilterPredicate.StringOperation.ENDS_WITH);
        predicate.setValue(FilterPredicateValue.fromString("_1"));
        onlineStatusFilter.setPredicate(predicate);
        @NotNull List<KeyFilter> keyFilters = Collections.singletonList(onlineStatusFilter);

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }

        long expectedEntitiesCnt = entityNameByTypeMap.entrySet()
                .stream()
                .filter(e -> !e.getKey().equals("building"))
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                .filter(e -> StringUtils.endsWith(e, "_1"))
                .count();
        Assert.assertEquals(expectedEntitiesCnt, loadedEntities.size());

        @NotNull Map<UUID, UUID> actualRelations = new HashMap<>();
        loadedEntities.forEach(ed -> {
            @NotNull UUID parentId = UUID.fromString(ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("parentId").getValue());
            UUID entityId = ed.getEntityId().getId();
            Assert.assertEquals(childParentRelationMap.get(entityId), parentId);
            actualRelations.put(entityId, parentId);

            String entityType = ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("type").getValue();
            String actualEntityName = ed.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue();
            String expectedEntityName = entityNameByTypeMap.get(entityType).get(entityId);
            Assert.assertEquals(expectedEntityName, actualEntityName);
        });

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
    }

    @Test
    public void testHierarchicalFindDevicesWithAttributesByQuery() throws ExecutionException, InterruptedException {
        @NotNull List<Asset> assets = new ArrayList<>();
        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Long> temperatures = new ArrayList<>();
        @NotNull List<Long> highTemperatures = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, new ArrayList<>(), new ArrayList<>(), temperatures, highTemperatures);

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull DeviceSearchQueryFilter filter = new DeviceSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Contains");
        filter.setMaxLevel(2);
        filter.setFetchLastLevelOnly(true);

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(25, loadedEntities.size());
        loadedEntities.forEach(entity -> Assert.assertTrue(devices.stream().map(Device::getId).collect(Collectors.toSet()).contains(entity.getEntityId())));
        @NotNull List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "temperature"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        @NotNull List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        @NotNull List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }


    @Test
    public void testHierarchicalFindAssetsWithAttributesByQuery() throws ExecutionException, InterruptedException {
        @NotNull List<Asset> assets = new ArrayList<>();
        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Long> consumptions = new ArrayList<>();
        @NotNull List<Long> highConsumptions = new ArrayList<>();
        createTestHierarchy(tenantId, assets, devices, consumptions, highConsumptions, new ArrayList<>(), new ArrayList<>());

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            attributeFutures.add(saveLongAttribute(asset.getId(), "consumption", consumptions.get(i), DataConstants.SERVER_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull AssetSearchQueryFilter filter = new AssetSearchQueryFilter();
        filter.setRootEntity(tenantId);
        filter.setDirection(EntitySearchDirection.FROM);
        filter.setRelationType("Manages");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(5, loadedEntities.size());
        @NotNull List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("consumption").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceTemperatures = consumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.ATTRIBUTE, "consumption"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(50));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        @NotNull List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highConsumptions.size(), loadedEntities.size());

        @NotNull List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ATTRIBUTE).get("consumption").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceHighTemperatures = highConsumptions.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    private void createTestHierarchy(TenantId tenantId, @NotNull List<Asset> assets, @NotNull List<Device> devices, @NotNull List<Long> consumptions, @NotNull List<Long> highConsumptions, @NotNull List<Long> temperatures, @NotNull List<Long> highTemperatures) throws InterruptedException {
        for (int i = 0; i < ENTITY_COUNT; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset" + i);
            asset.setType("type" + i);
            asset.setLabel("AssetLabel" + i);
            asset = assetService.saveAsset(asset);
            //TO make sure devices have different created time
            Thread.sleep(1);
            assets.add(asset);
            createRelation(tenantId, "Manages", tenantId, asset.getId());
            long consumption = (long) (Math.random() * 100);
            consumptions.add(consumption);
            if (consumption > 50) {
                highConsumptions.add(consumption);
            }

            //tenant -> asset : one-to-one but many edges
            for (int n = 0; n < ENTITY_COUNT; n++) {
                createRelation(tenantId, "UseCase-" + n, tenantId, asset.getId());
            }

            for (int j = 0; j < ENTITY_COUNT; j++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("A" + i + "Device" + j);
                device.setType("default" + j);
                device.setLabel("testLabel" + (int) (Math.random() * 1000));
                device = deviceService.saveDevice(device);
                //TO make sure devices have different created time
                Thread.sleep(1);
                devices.add(device);
                createRelation(tenantId, "Contains", asset.getId(), device.getId());
                long temperature = (long) (Math.random() * 100);
                temperatures.add(temperature);
                if (temperature > 45) {
                    highTemperatures.add(temperature);
                }

                //asset -> device : one-to-one but many edges
                for (int n = 0; n < ENTITY_COUNT; n++) {
                    createRelation(tenantId, "UseCase-" + n, asset.getId(), device.getId());
                }
            }
        }

        //asset -> device one-to-many shared with other assets
        for (int n = 0; n < devices.size(); n = n + ENTITY_COUNT) {
            createRelation(tenantId, "SharedWithAsset0", assets.get(0).getId(), devices.get(n).getId());
        }

        createManyCustomRelationsBetweenTwoNodes(tenantId, "UseCase", assets, devices);
        createHorizontalRingRelations(tenantId, "Ring(Loop)-Ast", assets);
        createLoopRelations(tenantId, "Loop-Tnt-Ast-Dev", tenantId, assets.get(0).getId(), devices.get(0).getId());
        createLoopRelations(tenantId, "Loop-Tnt-Ast", tenantId, assets.get(1).getId());
        createLoopRelations(tenantId, "Loop-Ast-Tnt-Ast", assets.get(2).getId(), tenantId, assets.get(3).getId());

        //printAllRelations();
    }

    @NotNull
    private ResultSetExtractor<List<List<String>>> getListResultSetExtractor() {
        return rs -> {
            @NotNull List<List<String>> list = new ArrayList<>();
            final int columnCount = rs.getMetaData().getColumnCount();
            @NotNull List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i));
            }
            list.add(columns);
            while (rs.next()) {
                @NotNull List<String> data = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    data.add(rs.getString(i));
                }
                list.add(data);
            }
            return list;
        };
    }

    /*
     * This useful to reproduce exact data in the PostgreSQL and play around with pgadmin query and analyze tool
     * */
    private void printAllRelations() {
        System.out.println("" +
                "DO\n" +
                "$$\n" +
                "    DECLARE\n" +
                "        someint integer;\n" +
                "    BEGIN\n" +
                "        DROP TABLE IF EXISTS relation_test;\n" +
                "        CREATE TABLE IF NOT EXISTS relation_test\n" +
                "        (\n" +
                "            from_id             uuid,\n" +
                "            from_type           varchar(255),\n" +
                "            to_id               uuid,\n" +
                "            to_type             varchar(255),\n" +
                "            relation_type_group varchar(255),\n" +
                "            relation_type       varchar(255),\n" +
                "            additional_info     varchar,\n" +
                "            CONSTRAINT relation_test_pkey PRIMARY KEY (from_id, from_type, relation_type_group, relation_type, to_id, to_type)\n" +
                "        );");

        relationRepository.findAll().forEach(r ->
                System.out.printf("INSERT INTO relation_test (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
                                " VALUES (%s, %s, %s, %s, %s, %s, %s);\n",
                        quote(r.getFromId()), quote(r.getFromType()), quote(r.getToId()), quote(r.getToType()),
                        quote(r.getRelationTypeGroup()), quote(r.getRelationType()), quote(r.getAdditionalInfo()))
        );

        System.out.println("" +
                "    END\n" +
                "$$;");
    }

    private String quote(@Nullable Object s) {
        return s == null ? null : "'" + s + "'";
    }

    void createLoopRelations(TenantId tenantId, String type, @NotNull EntityId... ids) {
        assertThat("ids lenght", ids.length, Matchers.greaterThanOrEqualTo(1));
        //chain all from the head to the tail
        for (int i = 1; i < ids.length; i++) {
            relationService.saveRelation(tenantId, new EntityRelation(ids[i - 1], ids[i], type, RelationTypeGroup.COMMON));
        }
        //chain tail -> head
        relationService.saveRelation(tenantId, new EntityRelation(ids[ids.length - 1], ids[0], type, RelationTypeGroup.COMMON));
    }

    void createHorizontalRingRelations(TenantId tenantId, String type, @NotNull List<Asset> assets) {
        createLoopRelations(tenantId, type, assets.stream().map(Asset::getId).toArray(EntityId[]::new));
    }

    void createManyCustomRelationsBetweenTwoNodes(TenantId tenantId, String type, @NotNull List<Asset> assets, @NotNull List<Device> devices) {
        for (int i = 1; i <= 5; i++) {
            @NotNull final String typeI = type + i;
            createOneToManyRelations(tenantId, typeI, tenantId, assets.stream().map(Asset::getId).collect(Collectors.toList()));
            assets.forEach(asset ->
                    createOneToManyRelations(tenantId, typeI, asset.getId(), devices.stream().map(Device::getId).collect(Collectors.toList())));
        }
    }

    void createOneToManyRelations(TenantId tenantId, String type, EntityId from, @NotNull List<EntityId> toIds) {
        toIds.forEach(toId -> createRelation(tenantId, type, from, toId));
    }

    void createRelation(TenantId tenantId, String type, EntityId from, EntityId toId) {
        relationService.saveRelation(tenantId, new EntityRelation(from, toId, type, RelationTypeGroup.COMMON));
    }


    @Test
    public void testSimpleFindEntityDataByQuery() throws InterruptedException {
        @NotNull List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            //TO make sure devices have different created time
            Thread.sleep(1);
            devices.add(deviceService.saveDevice(device));
        }

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        Assert.assertEquals(97, data.getTotalElements());
        Assert.assertEquals(10, data.getTotalPages());
        Assert.assertTrue(data.hasNext());
        Assert.assertEquals(10, data.getData().size());

        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(97, loadedEntities.size());

        @NotNull List<EntityId> loadedIds = loadedEntities.stream().map(EntityData::getEntityId).collect(Collectors.toList());
        @NotNull List<EntityId> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        deviceIds.sort(Comparator.comparing(EntityId::getId));
        loadedIds.sort(Comparator.comparing(EntityId::getId));
        Assert.assertEquals(deviceIds, loadedIds);

        @NotNull List<String> loadedNames = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Collections.sort(loadedNames);
        Collections.sort(deviceNames);
        Assert.assertEquals(deviceNames, loadedNames);

        sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), EntityDataSortOrder.Direction.DESC
        );

        pageLink = new EntityDataPageLink(10, 0, "device1", sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, null);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        Assert.assertEquals(11, data.getTotalElements());
        Assert.assertEquals("Device19", data.getData().get(0).getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityDataByQuery_operationEqual_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        String searchQuery = devices.get(2).getLabel();
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.NOT_EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size() - 1, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotEqual_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.NOT_EQUAL, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationStartsWith_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.STARTS_WITH, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationEndsWith_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.ENDS_WITH, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationContains_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "label-";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.NOT_CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    public void testFindEntityDataByQuery_operationNotContains_emptySearchQuery() {
        @NotNull List<Device> devices = createMockDevices(10);
        devices.get(0).setLabel("");
        devices.get(1).setLabel(null);
        devices.forEach(deviceService::saveDevice);

        @NotNull String searchQuery = "";
        @NotNull EntityDataQuery query = createDeviceSearchQuery("label", StringFilterPredicate.StringOperation.NOT_CONTAINS, searchQuery);

        PageData<EntityData> result = searchEntities(query);
        assertEquals(devices.size(), result.getTotalElements());
    }

    private PageData<EntityData> searchEntities(EntityDataQuery query) {
        return entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
    }

    @NotNull
    private EntityDataQuery createDeviceSearchQuery(String deviceField, StringFilterPredicate.StringOperation operation, String searchQuery) {
        @NotNull DeviceTypeFilter deviceTypeFilter = new DeviceTypeFilter();
        deviceTypeFilter.setDeviceType("default");
        deviceTypeFilter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(1000, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Arrays.asList(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                new EntityKey(EntityKeyType.ENTITY_FIELD, "label")
                                                             );

        @NotNull List<KeyFilter> keyFilters = createStringKeyFilters(deviceField, EntityKeyType.ENTITY_FIELD, operation, searchQuery);

        return new EntityDataQuery(deviceTypeFilter, pageLink, entityFields, null, keyFilters);
    }

    @NotNull
    private List<Device> createMockDevices(int count) {
        return Stream.iterate(1, i -> i + 1)
                .map(i -> {
                    @NotNull Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Device " + i);
                    device.setType("default");
                    device.setLabel("label-" + RandomUtils.nextInt(100, 10000));
                    return device;
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    @Test
    public void testFindEntityDataByQueryWithAttributes() throws ExecutionException, InterruptedException {

        @NotNull List<EntityKeyType> attributesEntityTypes = new ArrayList<>(Arrays.asList(EntityKeyType.CLIENT_ATTRIBUTE, EntityKeyType.SHARED_ATTRIBUTE, EntityKeyType.SERVER_ATTRIBUTE));

        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Long> temperatures = new ArrayList<>();
        @NotNull List<Long> highTemperatures = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
            if (temperature > 45) {
                highTemperatures.add(temperature);
            }
        }

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            for (String currentScope : DataConstants.allScopes()) {
                attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), currentScope));
            }
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        for (EntityKeyType currentAttributeKeyType : attributesEntityTypes) {
            @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(currentAttributeKeyType, "temperature"));
            EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
            PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
            while (data.hasNext()) {
                query = query.next();
                data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
                loadedEntities.addAll(data.getData());
            }
            Assert.assertEquals(67, loadedEntities.size());
            @NotNull List<String> loadedTemperatures = new ArrayList<>();
            for (@NotNull Device device : devices) {
                loadedTemperatures.add(loadedEntities.stream().filter(entityData -> entityData.getEntityId().equals(device.getId())).findFirst().orElse(null)
                        .getLatest().get(currentAttributeKeyType).get("temperature").getValue());
            }
            @NotNull List<String> deviceTemperatures = temperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());
            Assert.assertEquals(deviceTemperatures, loadedTemperatures);

            pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
            @NotNull KeyFilter highTemperatureFilter = createNumericKeyFilter("temperature", currentAttributeKeyType, NumericFilterPredicate.NumericOperation.GREATER, 45);
            @NotNull List<KeyFilter> keyFiltersHighTemperature = Collections.singletonList(highTemperatureFilter);

            query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersHighTemperature);

            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

            loadedEntities = new ArrayList<>(data.getData());

            while (data.hasNext()) {
                query = query.next();
                data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
                loadedEntities.addAll(data.getData());
            }
            Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

            @NotNull List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                    entityData.getLatest().get(currentAttributeKeyType).get("temperature").getValue()).collect(Collectors.toList());
            @NotNull List<String> deviceHighTemperatures = highTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

            Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        }
        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildNumericPredicateQueryOperations() throws ExecutionException, InterruptedException {

        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Long> temperatures = new ArrayList<>();
        @NotNull List<Long> equalTemperatures = new ArrayList<>();
        @NotNull List<Long> notEqualTemperatures = new ArrayList<>();
        @NotNull List<Long> greaterTemperatures = new ArrayList<>();
        @NotNull List<Long> greaterOrEqualTemperatures = new ArrayList<>();
        @NotNull List<Long> lessTemperatures = new ArrayList<>();
        @NotNull List<Long> lessOrEqualTemperatures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            long temperature = (long) (Math.random() * 100);
            temperatures.add(temperature);
            if (temperature == 45) {
                greaterOrEqualTemperatures.add(temperature);
                lessOrEqualTemperatures.add(temperature);
                equalTemperatures.add(temperature);
            } else if (temperature > 45) {
                greaterTemperatures.add(temperature);
                greaterOrEqualTemperatures.add(temperature);
                notEqualTemperatures.add(temperature);
            } else {
                lessTemperatures.add(temperature);
                lessOrEqualTemperatures.add(temperature);
                notEqualTemperatures.add(temperature);
            }
        }

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveLongAttribute(device.getId(), "temperature", temperatures.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );

        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "temperature"));

        @NotNull KeyFilter greaterTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.GREATER, 45);
        @NotNull List<KeyFilter> keyFiltersGreaterTemperature = Collections.singletonList(greaterTemperatureFilter);

        @NotNull KeyFilter greaterOrEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.GREATER_OR_EQUAL, 45);
        @NotNull List<KeyFilter> keyFiltersGreaterOrEqualTemperature = Collections.singletonList(greaterOrEqualTemperatureFilter);

        @NotNull KeyFilter lessTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.LESS, 45);
        @NotNull List<KeyFilter> keyFiltersLessTemperature = Collections.singletonList(lessTemperatureFilter);

        @NotNull KeyFilter lessOrEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.LESS_OR_EQUAL, 45);
        @NotNull List<KeyFilter> keyFiltersLessOrEqualTemperature = Collections.singletonList(lessOrEqualTemperatureFilter);

        @NotNull KeyFilter equalTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.EQUAL, 45);
        @NotNull List<KeyFilter> keyFiltersEqualTemperature = Collections.singletonList(equalTemperatureFilter);

        @NotNull KeyFilter notEqualTemperatureFilter = createNumericKeyFilter("temperature", EntityKeyType.CLIENT_ATTRIBUTE, NumericFilterPredicate.NumericOperation.NOT_EQUAL, 45);
        @NotNull List<KeyFilter> keyFiltersNotEqualTemperature = Collections.singletonList(notEqualTemperatureFilter);

        //Greater Operation

        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        @NotNull EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterTemperature);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(greaterTemperatures.size(), loadedEntities.size());

        @NotNull List<String> loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceTemperatures = greaterTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Greater or equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersGreaterOrEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(greaterOrEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = greaterOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Less Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(lessTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = lessTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Less or equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersLessOrEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(lessOrEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = lessOrEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(equalTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = equalTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        //Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualTemperature);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notEqualTemperatures.size(), loadedEntities.size());

        loadedTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("temperature").getValue()).collect(Collectors.toList());
        deviceTemperatures = notEqualTemperatures.stream().map(aLong -> Long.toString(aLong)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);


        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testFindEntityDataByQueryWithTimeseries() throws ExecutionException, InterruptedException {

        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<Double> temperatures = new ArrayList<>();
        @NotNull List<Double> highTemperatures = new ArrayList<>();
        for (int i = 0; i < 67; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            double temperature = Math.random() * 100.0;
            temperatures.add(temperature);
            if (temperature > 45.0) {
                highTemperatures.add(temperature);
            }
        }

        @NotNull List<ListenableFuture<Integer>> timeseriesFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            timeseriesFutures.add(saveLongTimeseries(device.getId(), "temperature", temperatures.get(i)));
        }
        Futures.allAsList(timeseriesFutures).get();

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.ASC
        );
        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));
        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));

        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, null);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(67, loadedEntities.size());
        @NotNull List<String> loadedTemperatures = new ArrayList<>();
        for (@NotNull Device device : devices) {
            loadedTemperatures.add(loadedEntities.stream().filter(entityData -> entityData.getEntityId().equals(device.getId())).findFirst().orElse(null)
                    .getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue());
        }
        @NotNull List<String> deviceTemperatures = temperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        Assert.assertEquals(deviceTemperatures, loadedTemperatures);

        pageLink = new EntityDataPageLink(10, 0, null, sortOrder);
        @NotNull KeyFilter highTemperatureFilter = new KeyFilter();
        highTemperatureFilter.setKey(new EntityKey(EntityKeyType.TIME_SERIES, "temperature"));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(45));
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        highTemperatureFilter.setPredicate(predicate);
        @NotNull List<KeyFilter> keyFilters = Collections.singletonList(highTemperatureFilter);

        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFilters);

        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);

        loadedEntities = new ArrayList<>(data.getData());
        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        Assert.assertEquals(highTemperatures.size(), loadedEntities.size());

        @NotNull List<String> loadedHighTemperatures = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.TIME_SERIES).get("temperature").getValue()).collect(Collectors.toList());
        @NotNull List<String> deviceHighTemperatures = highTemperatures.stream().map(aDouble -> Double.toString(aDouble)).collect(Collectors.toList());

        Assert.assertEquals(deviceHighTemperatures, loadedHighTemperatures);

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildStringPredicateQueryOperations() throws ExecutionException, InterruptedException {

        @NotNull List<Device> devices = new ArrayList<>();
        @NotNull List<String> attributeStrings = new ArrayList<>();
        @NotNull List<String> equalStrings = new ArrayList<>();
        @NotNull List<String> notEqualStrings = new ArrayList<>();
        @NotNull List<String> startsWithStrings = new ArrayList<>();
        @NotNull List<String> endsWithStrings = new ArrayList<>();
        @NotNull List<String> containsStrings = new ArrayList<>();
        @NotNull List<String> notContainsStrings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
            @NotNull List<StringFilterPredicate.StringOperation> operationValues = Arrays.asList(StringFilterPredicate.StringOperation.values());
            StringFilterPredicate.StringOperation operation = operationValues.get(new Random().nextInt(operationValues.size()));
            @NotNull String operationName = operation.name();
            attributeStrings.add(operationName);
            switch (operation) {
                case EQUAL:
                    equalStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    notEqualStrings.add(operationName);
                    break;
                case NOT_EQUAL:
                    notContainsStrings.add(operationName);
                    break;
                case STARTS_WITH:
                    notEqualStrings.add(operationName);
                    startsWithStrings.add(operationName);
                    endsWithStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case ENDS_WITH:
                    notEqualStrings.add(operationName);
                    endsWithStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case CONTAINS:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    containsStrings.add(operationName);
                    break;
                case NOT_CONTAINS:
                    notEqualStrings.add(operationName);
                    containsStrings.add(operationName);
                    break;
                case IN:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
                case NOT_IN:
                    notEqualStrings.add(operationName);
                    notContainsStrings.add(operationName);
                    break;
            }
        }

        @NotNull List<ListenableFuture<List<String>>> attributeFutures = new ArrayList<>();
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            attributeFutures.add(saveStringAttribute(device.getId(), "attributeString", attributeStrings.get(i), DataConstants.CLIENT_SCOPE));
        }
        Futures.allAsList(attributeFutures).get();

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC
        );

        @NotNull List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                                                              new EntityKey(EntityKeyType.ENTITY_FIELD, "entityType"));

        @NotNull List<EntityKey> latestValues = Collections.singletonList(new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, "attributeString"));

        @NotNull List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.EQUAL, "equal");

        @NotNull List<KeyFilter> keyFiltersNotEqualString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.NOT_EQUAL, "NOT_EQUAL");

        @NotNull List<KeyFilter> keyFiltersStartsWithString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.STARTS_WITH, "starts_");

        @NotNull List<KeyFilter> keyFiltersEndsWithString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.ENDS_WITH, "_WITH");

        @NotNull List<KeyFilter> keyFiltersContainsString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.CONTAINS, "contains");

        @NotNull List<KeyFilter> keyFiltersNotContainsString = createStringKeyFilters("attributeString", EntityKeyType.CLIENT_ATTRIBUTE, StringFilterPredicate.StringOperation.NOT_CONTAINS, "NOT_CONTAINS");

        @NotNull List<KeyFilter> deviceTypeFilters = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_EQUAL, "NOT_EQUAL");

        // Equal Operation

        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        @NotNull EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEqualString);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(equalStrings.size(), loadedEntities.size());

        @NotNull List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(equalStrings, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotEqualString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notEqualStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notEqualStrings, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersStartsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(startsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(startsWithStrings, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersEndsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(endsWithStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(endsWithStrings, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(containsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(containsStrings, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, keyFiltersNotContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(notContainsStrings.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.CLIENT_ATTRIBUTE).get("attributeString").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(notContainsStrings, loadedStrings));

        // Device type filters Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, latestValues, deviceTypeFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildStringPredicateQueryOperationsForEntityType() throws ExecutionException, InterruptedException {

        @NotNull List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
        }

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(
                new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"), EntityDataSortOrder.Direction.DESC
        );

        @NotNull List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                                                              new EntityKey(EntityKeyType.ENTITY_FIELD, "entityType"));

        @NotNull List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.EQUAL, "device");
        @NotNull List<KeyFilter> keyFiltersNotEqualString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_EQUAL, "asset");
        @NotNull List<KeyFilter> keyFiltersStartsWithString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "dev");
        @NotNull List<KeyFilter> keyFiltersEndsWithString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.ENDS_WITH, "ice");
        @NotNull List<KeyFilter> keyFiltersContainsString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.CONTAINS, "vic");
        @NotNull List<KeyFilter> keyFiltersNotContainsString = createStringKeyFilters("entityType", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.NOT_CONTAINS, "dolphin");

        // Equal Operation

        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        @NotNull EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersEqualString);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        @NotNull List<String> loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        @NotNull List<String> devicesNames = devices.stream().map(Device::getName).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not equal Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotEqualString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Starts with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersStartsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Ends with Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersEndsWithString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        // Not contains Operation

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, keyFiltersNotContainsString);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        loadedStrings = loadedEntities.stream().map(entityData ->
                entityData.getLatest().get(EntityKeyType.ENTITY_FIELD).get("name").getValue()).collect(Collectors.toList());

        Assert.assertTrue(listEqualWithoutOrder(devicesNames, loadedStrings));

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @Test
    public void testBuildSimplePredicateQueryOperations() throws InterruptedException {

        @NotNull List<Device> devices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            @NotNull Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            devices.add(deviceService.saveDevice(device));
            //TO make sure devices have different created time
            Thread.sleep(1);
        }

        @NotNull DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceType("default");
        filter.setDeviceNameFilter("");

        @NotNull EntityDataSortOrder sortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"), EntityDataSortOrder.Direction.DESC);

        @NotNull List<KeyFilter> deviceTypeFilters = createStringKeyFilters("type", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.EQUAL, "default");

        @NotNull KeyFilter createdTimeFilter = createNumericKeyFilter("createdTime", EntityKeyType.ENTITY_FIELD, NumericFilterPredicate.NumericOperation.GREATER, 1L);
        @NotNull List<KeyFilter> createdTimeFilters = Collections.singletonList(createdTimeFilter);

        @NotNull List<KeyFilter> nameFilters = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.CONTAINS, "Device");

        @NotNull List<EntityKey> entityFields = Arrays.asList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
                                                              new EntityKey(EntityKeyType.ENTITY_FIELD, "type"));

        // Device type filters

        @NotNull EntityDataPageLink pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        @NotNull EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, deviceTypeFilters);
        PageData<EntityData> data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        @NotNull List<EntityData> loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device create time filters

        pageLink = new EntityDataPageLink(100, 0, null, sortOrder);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, createdTimeFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        // Device name filters

        pageLink = new EntityDataPageLink(100, 0, null, null);
        query = new EntityDataQuery(filter, pageLink, entityFields, null, nameFilters);
        data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
        loadedEntities = getLoadedEntities(data, query);
        Assert.assertEquals(devices.size(), loadedEntities.size());

        deviceService.deleteDevicesByTenantId(tenantId);
    }

    @NotNull
    private Boolean listEqualWithoutOrder(@NotNull List<String> A, @NotNull List<String> B) {
        return A.containsAll(B) && B.containsAll(A);
    }

    @NotNull
    private List<EntityData> getLoadedEntities(@NotNull PageData<EntityData> data, EntityDataQuery query) {
        @NotNull List<EntityData> loadedEntities = new ArrayList<>(data.getData());

        while (data.hasNext()) {
            query = query.next();
            data = entityService.findEntityDataByQuery(tenantId, new CustomerId(CustomerId.NULL_UUID), query);
            loadedEntities.addAll(data.getData());
        }
        return loadedEntities;
    }

    @NotNull
    private List<KeyFilter> createStringKeyFilters(String key, EntityKeyType keyType, StringFilterPredicate.StringOperation operation, String value) {
        @NotNull KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        @NotNull StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString(value));
        predicate.setOperation(operation);
        predicate.setIgnoreCase(true);
        filter.setPredicate(predicate);
        return Collections.singletonList(filter);
    }

    @NotNull
    private KeyFilter createNumericKeyFilter(String key, EntityKeyType keyType, NumericFilterPredicate.NumericOperation operation, double value) {
        @NotNull KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        @NotNull NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromDouble(value));
        predicate.setOperation(operation);
        filter.setPredicate(predicate);

        return filter;
    }

    private ListenableFuture<List<String>> saveLongAttribute(EntityId entityId, String key, long value, String scope) {
        @NotNull KvEntry attrValue = new LongDataEntry(key, value);
        @NotNull AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<List<String>> saveStringAttribute(EntityId entityId, String key, String value, String scope) {
        @NotNull KvEntry attrValue = new StringDataEntry(key, value);
        @NotNull AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        return attributesService.save(SYSTEM_TENANT_ID, entityId, scope, Collections.singletonList(attr));
    }

    private ListenableFuture<Integer> saveLongTimeseries(EntityId entityId, String key, Double value) {
        @NotNull TsKvEntity tsKv = new TsKvEntity();
        tsKv.setStrKey(key);
        tsKv.setDoubleValue(value);
        @NotNull KvEntry telemetryValue = new DoubleDataEntry(key, value);
        @NotNull BasicTsKvEntry timeseries = new BasicTsKvEntry(42L, telemetryValue);
        return timeseriesService.save(SYSTEM_TENANT_ID, entityId, timeseries);
    }

    private void createMultiRootHierarchy(@NotNull List<Asset> buildings, @NotNull List<Asset> apartments,
                                          @NotNull Map<String, Map<UUID, String>> entityNameByTypeMap,
                                          @NotNull Map<UUID, UUID> childParentRelationMap) throws InterruptedException {
        for (int k = 0; k < 3; k++) {
            Asset building = new Asset();
            building.setTenantId(tenantId);
            building.setName("Building _" + k);
            building.setType("building");
            building.setLabel("building label" + k);
            building = assetService.saveAsset(building);
            buildings.add(building);
            entityNameByTypeMap.computeIfAbsent(building.getType(), n -> new HashMap<>()).put(building.getId().getId(), building.getName());

            for (int i = 0; i < 3; i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("Apt " + k + "_" + i);
                asset.setType("apartment");
                asset.setLabel("apartment " + i);
                asset = assetService.saveAsset(asset);
                //TO make sure devices have different created time
                Thread.sleep(1);
                entityNameByTypeMap.computeIfAbsent(asset.getType(), n -> new HashMap<>()).put(asset.getId().getId(), asset.getName());
                apartments.add(asset);
                @NotNull EntityRelation er = new EntityRelation();
                er.setFrom(building.getId());
                er.setTo(asset.getId());
                er.setType("buildingToApt");
                er.setTypeGroup(RelationTypeGroup.COMMON);
                relationService.saveRelation(tenantId, er);
                childParentRelationMap.put(asset.getUuidId(), building.getUuidId());
                for (int j = 0; j < 3; j++) {
                    Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Heat" + k + "_" + i + "_" + j);
                    device.setType("heatmeter");
                    device.setLabel("heatmeter" + (int) (Math.random() * 1000));
                    device = deviceService.saveDevice(device);
                    //TO make sure devices have different created time
                    Thread.sleep(1);
                    entityNameByTypeMap.computeIfAbsent(device.getType(), n -> new HashMap<>()).put(device.getId().getId(), device.getName());
                    er = new EntityRelation();
                    er.setFrom(asset.getId());
                    er.setTo(device.getId());
                    er.setType("AptToHeat");
                    er.setTypeGroup(RelationTypeGroup.COMMON);
                    relationService.saveRelation(tenantId, er);
                    childParentRelationMap.put(device.getUuidId(), asset.getUuidId());
                }

                for (int j = 0; j < 3; j++) {
                    Device device = new Device();
                    device.setTenantId(tenantId);
                    device.setName("Energy" + k + "_" + i + "_" + j);
                    device.setType("energymeter");
                    device.setLabel("energymeter" + (int) (Math.random() * 1000));
                    device = deviceService.saveDevice(device);
                    //TO make sure devices have different created time
                    Thread.sleep(1);
                    entityNameByTypeMap.computeIfAbsent(device.getType(), n -> new HashMap<>()).put(device.getId().getId(), device.getName());
                    er = new EntityRelation();
                    er.setFrom(asset.getId());
                    er.setTo(device.getId());
                    er.setType("AptToEnergy");
                    er.setTypeGroup(RelationTypeGroup.COMMON);
                    relationService.saveRelation(tenantId, er);
                    childParentRelationMap.put(device.getUuidId(), asset.getUuidId());
                }
            }
        }
    }
}
