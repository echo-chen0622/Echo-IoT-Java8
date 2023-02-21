package org.echoiot.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.AssetId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.relation.*;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class BaseRelationServiceTest extends AbstractServiceTest {

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    @Test
    public void testSaveRelation() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentId = new AssetId(Uuids.timeBased());
        @NotNull AssetId childId = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relation = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        Assert.assertTrue(saveRelation(relation));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, parentId, "NOT_EXISTING_TYPE", RelationTypeGroup.COMMON));
    }

    @Test
    public void testDeleteRelation() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentId = new AssetId(Uuids.timeBased());
        @NotNull AssetId childId = new AssetId(Uuids.timeBased());
        @NotNull AssetId subChildId = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA).get());

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertTrue(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).get());
    }

    @Test
    public void testDeleteRelationConcurrently() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentId = new AssetId(Uuids.timeBased());
        @NotNull AssetId childId = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);

        @NotNull List<ListenableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(relationService.deleteRelationAsync(SYSTEM_TENANT_ID, relationA));
        }
        List<Boolean> results = Futures.allAsList(futures).get();
        Assert.assertTrue(results.contains(true));
    }

    @Test
    public void testDeleteEntityRelations() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentId = new AssetId(Uuids.timeBased());
        @NotNull AssetId childId = new AssetId(Uuids.timeBased());
        @NotNull AssetId subChildId = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(parentId, childId, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(childId, subChildId, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);

        relationService.deleteEntityRelations(SYSTEM_TENANT_ID, childId);

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, parentId, childId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));

        Assert.assertFalse(relationService.checkRelation(SYSTEM_TENANT_ID, childId, subChildId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
    }

    @Test
    public void testFindFrom() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentA = new AssetId(Uuids.timeBased());
        @NotNull AssetId parentB = new AssetId(Uuids.timeBased());
        @NotNull AssetId childA = new AssetId(Uuids.timeBased());
        @NotNull AssetId childB = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        @NotNull EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        @NotNull EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (@NotNull EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.CONTAINS_TYPE, relation.getType());
            Assert.assertEquals(parentA, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFrom(SYSTEM_TENANT_ID, parentB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (@NotNull EntityRelation relation : relations) {
            Assert.assertEquals(EntityRelation.MANAGES_TYPE, relation.getType());
            Assert.assertEquals(parentB, relation.getFrom());
            Assert.assertTrue(childA.equals(relation.getTo()) || childB.equals(relation.getTo()));
        }

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByFromAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());
    }

    @NotNull
    private Boolean saveRelation(EntityRelation relationA1) {
        return relationService.saveRelation(SYSTEM_TENANT_ID, relationA1);
    }

    @Test
    public void testFindTo() throws ExecutionException, InterruptedException {
        @NotNull AssetId parentA = new AssetId(Uuids.timeBased());
        @NotNull AssetId parentB = new AssetId(Uuids.timeBased());
        @NotNull AssetId childA = new AssetId(Uuids.timeBased());
        @NotNull AssetId childB = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA1 = new EntityRelation(parentA, childA, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationA2 = new EntityRelation(parentA, childB, EntityRelation.CONTAINS_TYPE);

        @NotNull EntityRelation relationB1 = new EntityRelation(parentB, childA, EntityRelation.MANAGES_TYPE);
        @NotNull EntityRelation relationB2 = new EntityRelation(parentB, childB, EntityRelation.MANAGES_TYPE);

        saveRelation(relationA1);
        saveRelation(relationA2);

        saveRelation(relationB1);
        saveRelation(relationB2);

        List<EntityRelation> relations = relationService.findByTo(SYSTEM_TENANT_ID, childA, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (@NotNull EntityRelation relation : relations) {
            Assert.assertEquals(childA, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childA, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, childB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(1, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentA, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByToAndType(SYSTEM_TENANT_ID, parentB, EntityRelation.MANAGES_TYPE, RelationTypeGroup.COMMON);
        Assert.assertEquals(0, relations.size());

        relations = relationService.findByTo(SYSTEM_TENANT_ID, childB, RelationTypeGroup.COMMON);
        Assert.assertEquals(2, relations.size());
        for (@NotNull EntityRelation relation : relations) {
            Assert.assertEquals(childB, relation.getTo());
            Assert.assertTrue(parentA.equals(relation.getFrom()) || parentB.equals(relation.getFrom()));
        }
    }

    @Test
    public void testCyclicRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> C -> A
        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetA, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
    }

    @Test
    public void testRecursiveRelation() throws ExecutionException, InterruptedException {
        // A -> B -> [C,D]
        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull DeviceId deviceD = new DeviceId(Uuids.timeBased());

        @NotNull EntityRelation relationAB = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationBC = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationBD = new EntityRelation(assetB, deviceD, EntityRelation.CONTAINS_TYPE);


        saveRelation(relationAB);
        saveRelation(relationBC);
        saveRelation(relationBD);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(2, relations.size());
        Assert.assertTrue(relations.contains(relationAB));
        Assert.assertTrue(relations.contains(relationBC));
    }

    @Test
    public void testRecursiveRelationDepth() throws ExecutionException, InterruptedException {
        int maxLevel = 1000;
        @NotNull AssetId root = new AssetId(Uuids.timeBased());
        @NotNull AssetId left = new AssetId(Uuids.timeBased());
        @NotNull AssetId right = new AssetId(Uuids.timeBased());

        @NotNull List<EntityRelation> expected = new ArrayList<>();

        @NotNull EntityRelation relationAB = new EntityRelation(root, left, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationBC = new EntityRelation(root, right, EntityRelation.CONTAINS_TYPE);
        saveRelation(relationAB);
        expected.add(relationAB);

        saveRelation(relationBC);
        expected.add(relationBC);

        for (int i = 0; i < maxLevel; i++) {
            @NotNull var newLeft = new AssetId(Uuids.timeBased());
            @NotNull var newRight = new AssetId(Uuids.timeBased());
            @NotNull EntityRelation relationLeft = new EntityRelation(left, newLeft, EntityRelation.CONTAINS_TYPE);
            @NotNull EntityRelation relationRight = new EntityRelation(right, newRight, EntityRelation.CONTAINS_TYPE);
            saveRelation(relationLeft);
            expected.add(relationLeft);
            saveRelation(relationRight);
            expected.add(relationRight);
            left = newLeft;
            right = newRight;
        }


        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(root, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expected.size(), relations.size());
        for(EntityRelation r : expected){
            Assert.assertTrue(relations.contains(r));
        }
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyFrom() throws ExecutionException, InterruptedException {
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setTo(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(saveRelation(relation));
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyTo() throws ExecutionException, InterruptedException {
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setType(EntityRelation.CONTAINS_TYPE);
        Assert.assertTrue(saveRelation(relation));
    }

    @Test(expected = DataValidationException.class)
    public void testSaveRelationWithEmptyType() throws ExecutionException, InterruptedException {
        @NotNull EntityRelation relation = new EntityRelation();
        relation.setFrom(new AssetId(Uuids.timeBased()));
        relation.setTo(new AssetId(Uuids.timeBased()));
        Assert.assertTrue(saveRelation(relation));
    }

    @Test
    public void testFindByQueryFetchLastOnlyTreeLike() throws Exception {
        // A -> B
        // A -> C
        // C -> D
        // C -> E

        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetD = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetE = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    public void testFindByQueryFetchLastOnlySingleLinked() throws Exception {
        // A -> B -> C -> D

        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetD = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetB, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(1, relations.size());
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertFalse(relations.contains(relationA));
        Assert.assertFalse(relations.contains(relationB));
    }

    @Test
    public void testFindByQueryFetchLastOnlyTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetD = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetE = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetF = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetG = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(3, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationB));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    public void testFindByQueryTreeLikeWithMaxLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetD = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetE = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetF = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetG = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, 2, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(4, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertFalse(relations.contains(relationE));
        Assert.assertFalse(relations.contains(relationF));
    }

    @Test
    public void testFindByQueryTreeLikeWithUnlimLvl() throws Exception {
        // A -> B   A
        // A -> C   B
        // C -> D   C
        // C -> E   D
        // D -> F   E
        // D -> G   F

        @NotNull AssetId assetA = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetB = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetC = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetD = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetE = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetF = new AssetId(Uuids.timeBased());
        @NotNull AssetId assetG = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation relationA = new EntityRelation(assetA, assetB, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationB = new EntityRelation(assetA, assetC, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationC = new EntityRelation(assetC, assetD, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationD = new EntityRelation(assetC, assetE, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationE = new EntityRelation(assetD, assetF, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation relationF = new EntityRelation(assetD, assetG, EntityRelation.CONTAINS_TYPE);

        saveRelation(relationA);
        saveRelation(relationB);
        saveRelation(relationC);
        saveRelation(relationD);
        saveRelation(relationE);
        saveRelation(relationF);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(assetA, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(6, relations.size());
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));

        //Test from cache
        relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertTrue(relations.contains(relationA));
        Assert.assertTrue(relations.contains(relationB));
        Assert.assertTrue(relations.contains(relationC));
        Assert.assertTrue(relations.contains(relationD));
        Assert.assertTrue(relations.contains(relationE));
        Assert.assertTrue(relations.contains(relationF));
    }

    @Test
    public void testFindByQueryLargeHierarchyFetchAllWithUnlimLvl() throws Exception {
        @NotNull AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        @NotNull List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, false);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, false));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    @Test
    public void testFindByQueryLargeHierarchyFetchLastOnlyWithUnlimLvl() throws Exception {
        @NotNull AssetId rootAsset = new AssetId(Uuids.timeBased());
        final int hierarchyLvl = 10;
        @NotNull List<EntityRelation> expectedRelations = new LinkedList<>();

        createAssetRelationsRecursively(rootAsset, hierarchyLvl, expectedRelations, true);

        @NotNull EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(rootAsset, EntitySearchDirection.FROM, -1, true));
        query.setFilters(Collections.singletonList(new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.singletonList(EntityType.ASSET))));
        List<EntityRelation> relations = relationService.findByQuery(SYSTEM_TENANT_ID, query).get();
        Assert.assertEquals(expectedRelations.size(), relations.size());
        Assert.assertTrue(relations.containsAll(expectedRelations));
    }

    private void createAssetRelationsRecursively(AssetId rootAsset, int lvl, @NotNull List<EntityRelation> entityRelations, boolean lastLvlOnly) throws Exception {
        if (lvl == 0) return;

        @NotNull AssetId firstAsset = new AssetId(Uuids.timeBased());
        @NotNull AssetId secondAsset = new AssetId(Uuids.timeBased());

        @NotNull EntityRelation firstRelation = new EntityRelation(rootAsset, firstAsset, EntityRelation.CONTAINS_TYPE);
        @NotNull EntityRelation secondRelation = new EntityRelation(rootAsset, secondAsset, EntityRelation.CONTAINS_TYPE);

        saveRelation(firstRelation);
        saveRelation(secondRelation);

        if (!lastLvlOnly || lvl == 1) {
            entityRelations.add(firstRelation);
            entityRelations.add(secondRelation);
        }

        createAssetRelationsRecursively(firstAsset, lvl - 1, entityRelations, lastLvlOnly);
        createAssetRelationsRecursively(secondAsset, lvl - 1, entityRelations, lastLvlOnly);
    }
}
