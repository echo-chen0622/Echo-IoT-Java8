package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.CacheConstants.RELATIONS_CACHE;

public abstract class BaseRelationCacheTest extends AbstractServiceTest {

    private static final EntityId ENTITY_ID_FROM = new DeviceId(UUID.randomUUID());
    private static final EntityId ENTITY_ID_TO = new DeviceId(UUID.randomUUID());
    private static final String RELATION_TYPE = "Contains";

    @Autowired
    private RelationService relationService;
    @Autowired
    private CacheManager cacheManager;

    private RelationDao relationDao;

    @Before
    public void setup() throws Exception {
        relationDao = mock(RelationDao.class);
        ReflectionTestUtils.setField(unwrapRelationService(), "relationDao", relationDao);
    }

    @After
    public void cleanup() {
        cacheManager.getCache(RELATIONS_CACHE).clear();
    }

    private RelationService unwrapRelationService() throws Exception {
        if (AopUtils.isAopProxy(relationService) && relationService instanceof Advised) {
            Object target = ((Advised) relationService).getTargetSource().getTarget();
            return (RelationService) target;
        }
        return relationService;
    }

    @Test
    public void testFindRelationByFrom_Cached() throws ExecutionException, InterruptedException {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }

    @Test
    public void testDeleteRelations_EvictsCache() {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.deleteRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(2)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }
}
