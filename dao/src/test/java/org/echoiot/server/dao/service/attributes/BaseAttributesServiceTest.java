package org.echoiot.server.dao.service.attributes;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cache.TbTransactionalCache;
import org.echoiot.server.common.data.DataConstants;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.common.data.kv.BaseAttributeKvEntry;
import org.echoiot.server.common.data.kv.KvEntry;
import org.echoiot.server.common.data.kv.StringDataEntry;
import org.echoiot.server.dao.attributes.AttributeCacheKey;
import org.echoiot.server.dao.attributes.AttributesService;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public abstract class BaseAttributesServiceTest extends AbstractServiceTest {

    private static final String OLD_VALUE = "OLD VALUE";
    private static final String NEW_VALUE = "NEW VALUE";

    @Resource
    private TbTransactionalCache<AttributeCacheKey, AttributeKvEntry> cache;

    @Resource
    private AttributesService attributesService;

    @Before
    public void before() {
    }

    @Test
    public void saveAndFetch() throws Exception {
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());
        @NotNull KvEntry attrValue = new StringDataEntry("attribute1", "value1");
        @NotNull AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attr)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attr.getKey()).get();
        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attr, saved.get());
    }

    @Test
    public void saveMultipleTypeAndFetch() throws Exception {
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());
        @NotNull KvEntry attrOldValue = new StringDataEntry("attribute1", "value1");
        @NotNull AttributeKvEntry attrOld = new BaseAttributeKvEntry(attrOldValue, 42L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrOld)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();

        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attrOld, saved.get());

        @NotNull KvEntry attrNewValue = new StringDataEntry("attribute1", "value2");
        @NotNull AttributeKvEntry attrNew = new BaseAttributeKvEntry(attrNewValue, 73L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrNew)).get();

        saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();
        Assert.assertEquals(attrNew, saved.get());
    }

    @Test
    public void findAll() throws Exception {
        @NotNull DeviceId deviceId = new DeviceId(Uuids.timeBased());

        @NotNull KvEntry attrAOldValue = new StringDataEntry("A", "value1");
        @NotNull AttributeKvEntry attrAOld = new BaseAttributeKvEntry(attrAOldValue, 42L);
        @NotNull KvEntry attrANewValue = new StringDataEntry("A", "value2");
        @NotNull AttributeKvEntry attrANew = new BaseAttributeKvEntry(attrANewValue, 73L);
        @NotNull KvEntry attrBNewValue = new StringDataEntry("B", "value3");
        @NotNull AttributeKvEntry attrBNew = new BaseAttributeKvEntry(attrBNewValue, 73L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrAOld)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrANew)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrBNew)).get();

        List<AttributeKvEntry> saved = attributesService.findAll(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE).get();

        Assert.assertNotNull(saved);
        Assert.assertEquals(2, saved.size());

        Assert.assertEquals(attrANew, saved.get(0));
        Assert.assertEquals(attrBNew, saved.get(1));
    }

    @Test
    public void testDummyRequestWithEmptyResult() throws Exception {
        var future = attributesService.find(new TenantId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), DataConstants.SERVER_SCOPE, "TEST");
        Assert.assertNotNull(future);
        var result = future.get(10, TimeUnit.SECONDS);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConcurrentTransaction() throws Exception {
        @NotNull var tenantId = new TenantId(UUID.randomUUID());
        @NotNull var deviceId = new DeviceId(UUID.randomUUID());
        @NotNull var scope = DataConstants.SERVER_SCOPE;
        @NotNull var key = "TEST";

        @NotNull var attrKey = new AttributeCacheKey(scope, deviceId, "TEST");
        @NotNull var oldValue = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, OLD_VALUE));
        @NotNull var newValue = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, NEW_VALUE));

        var trx = cache.newTransactionForKey(attrKey);
        cache.putIfAbsent(attrKey, newValue);
        trx.putIfAbsent(attrKey, oldValue);
        Assert.assertFalse(trx.commit());
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    @Test
    public void testConcurrentFetchAndUpdate() throws Exception {
        @NotNull var tenantId = new TenantId(UUID.randomUUID());
        @NotNull ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                @NotNull var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdate(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testConcurrentFetchAndUpdateMulti() throws Exception {
        @NotNull var tenantId = new TenantId(UUID.randomUUID());
        @NotNull ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                @NotNull var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdateMulti(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void testFetchAndUpdateEmpty() throws Exception {
        @NotNull var tenantId = new TenantId(UUID.randomUUID());
        @NotNull var deviceId = new DeviceId(UUID.randomUUID());
        @NotNull var scope = DataConstants.SERVER_SCOPE;
        @NotNull var key = "TEST";

        Optional<AttributeKvEntry> emptyValue = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
        Assert.assertTrue(emptyValue.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE);
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    @Test
    public void testFetchAndUpdateMulti() throws Exception {
        @NotNull var tenantId = new TenantId(UUID.randomUUID());
        @NotNull var deviceId = new DeviceId(UUID.randomUUID());
        @NotNull var scope = DataConstants.SERVER_SCOPE;
        @NotNull var key1 = "TEST1";
        @NotNull var key2 = "TEST2";

        @NotNull var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertTrue(value.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(1, value.size());
        Assert.assertEquals(OLD_VALUE, value.get(0));

        saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertTrue(value.contains(OLD_VALUE));
        Assert.assertTrue(value.contains(NEW_VALUE));

        saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertEquals(NEW_VALUE, value.get(0));
        Assert.assertEquals(NEW_VALUE, value.get(1));
    }

    private void testConcurrentFetchAndUpdate(TenantId tenantId, DeviceId deviceId, @NotNull ListeningExecutorService pool) throws Exception {
        @NotNull var scope = DataConstants.SERVER_SCOPE;
        @NotNull var key = "TEST";
        saveAttribute(tenantId, deviceId, scope, key, OLD_VALUE);
        @NotNull List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            @NotNull var value = getAttributeValue(tenantId, deviceId, scope, key);
            Assert.assertTrue(value.equals(OLD_VALUE) || value.equals(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE)));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    private void testConcurrentFetchAndUpdateMulti(TenantId tenantId, DeviceId deviceId, @NotNull ListeningExecutorService pool) throws Exception {
        @NotNull var scope = DataConstants.SERVER_SCOPE;
        @NotNull var key1 = "TEST1";
        @NotNull var key2 = "TEST2";
        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);
        saveAttribute(tenantId, deviceId, scope, key2, OLD_VALUE);
        @NotNull List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            @NotNull var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
            Assert.assertEquals(2, value.size());
            Assert.assertTrue(value.contains(OLD_VALUE) || value.contains(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> {
            saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);
            saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);
        }));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);
        @NotNull var newResult = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, newResult.size());
        Assert.assertEquals(NEW_VALUE, newResult.get(0));
        Assert.assertEquals(NEW_VALUE, newResult.get(1));
    }

    @NotNull
    private String getAttributeValue(TenantId tenantId, DeviceId deviceId, String scope, String key) {
        try {
            Optional<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
            return entry.orElseThrow(RuntimeException::new).getStrValue().orElse("Unknown");
        } catch (Exception e) {
            log.warn("Failed to get attribute", e.getCause());
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private List<String> getAttributeValues(TenantId tenantId, DeviceId deviceId, String scope, List<String> keys) {
        try {
            List<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, keys).get(10, TimeUnit.SECONDS);
            return entry.stream().map(e -> e.getStrValue().orElse(null)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get attributes", e.getCause());
            throw new RuntimeException(e);
        }
    }

    private void saveAttribute(TenantId tenantId, DeviceId deviceId, String scope, String key, String s) {
        try {
            @NotNull AttributeKvEntry newEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, s));
            attributesService.save(tenantId, deviceId, scope, Collections.singletonList(newEntry)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to save attribute", e.getCause());
            Assert.assertNull(e);
        }
    }


}
