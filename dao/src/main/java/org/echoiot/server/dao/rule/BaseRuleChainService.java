package org.echoiot.server.dao.rule;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.id.*;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.relation.EntityRelation;
import org.echoiot.server.common.data.relation.RelationTypeGroup;
import org.echoiot.server.common.data.rule.*;
import org.echoiot.server.dao.entity.AbstractEntityService;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.dao.service.DataValidator;
import org.echoiot.server.dao.service.PaginatedRemover;
import org.echoiot.server.dao.service.Validator;
import org.echoiot.server.dao.service.validator.RuleChainDataValidator;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.echoiot.server.common.data.DataConstants.TENANT;
import static org.echoiot.server.dao.service.Validator.validateId;

/**
 * Created by igor on 3/12/18.
 */
@Service
@Slf4j
public class BaseRuleChainService extends AbstractEntityService implements RuleChainService {

    private static final int DEFAULT_PAGE_SIZE = 1000;

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String TB_RULE_CHAIN_INPUT_NODE = "org.echoiot.rule.engine.flow.TbRuleChainInputNode";
    @Resource
    private RuleChainDao ruleChainDao;

    @Resource
    private RuleNodeDao ruleNodeDao;

    @Resource
    private DataValidator<RuleChain> ruleChainValidator;

    @Override
    @Transactional
    public RuleChain saveRuleChain(@NotNull RuleChain ruleChain) {
        ruleChainValidator.validate(ruleChain, RuleChain::getTenantId);
        try {
            return ruleChainDao.save(ruleChain.getTenantId(), ruleChain);
        } catch (Exception e) {
            checkConstraintViolation(e, "rule_chain_external_id_unq_key", "Rule Chain with such external id already exists!");
            throw e;
        }
    }

    @Override
    @Transactional
    public boolean setRootRuleChain(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (!ruleChain.isRoot()) {
            RuleChain previousRootRuleChain = getRootTenantRuleChain(ruleChain.getTenantId());
            if (previousRootRuleChain == null) {
                setRootAndSave(tenantId, ruleChain);
                return true;
            } else if (!previousRootRuleChain.getId().equals(ruleChain.getId())) {
                previousRootRuleChain.setRoot(false);
                ruleChainDao.save(tenantId, previousRootRuleChain);
                setRootAndSave(tenantId, ruleChain);
                return true;
            }
        }
        return false;
    }

    private void setRootAndSave(TenantId tenantId, @NotNull RuleChain ruleChain) {
        ruleChain.setRoot(true);
        ruleChainDao.save(tenantId, ruleChain);
    }

    @NotNull
    @Override
    public RuleChainUpdateResult saveRuleChainMetaData(TenantId tenantId, @NotNull RuleChainMetaData ruleChainMetaData) {
        Validator.validateId(ruleChainMetaData.getRuleChainId(), "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainMetaData.getRuleChainId());
        if (ruleChain == null) {
            return RuleChainUpdateResult.failed();
        }
        RuleChainDataValidator.validateMetaData(ruleChainMetaData);

        List<RuleNode> nodes = ruleChainMetaData.getNodes();
        @NotNull List<RuleNode> toAddOrUpdate = new ArrayList<>();
        @NotNull List<RuleNode> toDelete = new ArrayList<>();
        @NotNull List<EntityRelation> relations = new ArrayList<>();

        @NotNull Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        if (nodes != null) {
            for (@NotNull RuleNode node : nodes) {
                if (node.getId() != null) {
                    ruleNodeIndexMap.put(node.getId(), nodes.indexOf(node));
                } else {
                    toAddOrUpdate.add(node);
                }
            }
        }

        @NotNull List<RuleNodeUpdateResult> updatedRuleNodes = new ArrayList<>();
        @NotNull List<RuleNode> existingRuleNodes = getRuleChainNodes(tenantId, ruleChainMetaData.getRuleChainId());
        for (@NotNull RuleNode existingNode : existingRuleNodes) {
            deleteEntityRelations(tenantId, existingNode.getId());
            Integer index = ruleNodeIndexMap.get(existingNode.getId());
            @Nullable RuleNode newRuleNode = null;
            if (index != null) {
                newRuleNode = ruleChainMetaData.getNodes().get(index);
                toAddOrUpdate.add(newRuleNode);
            } else {
                updatedRuleNodes.add(new RuleNodeUpdateResult(existingNode, null));
                toDelete.add(existingNode);
            }
            updatedRuleNodes.add(new RuleNodeUpdateResult(existingNode, newRuleNode));
        }
        if (nodes != null) {
            for (@NotNull RuleNode node : toAddOrUpdate) {
                node.setRuleChainId(ruleChain.getId());
                RuleNode savedNode = ruleNodeDao.save(tenantId, node);
                relations.add(new EntityRelation(ruleChainMetaData.getRuleChainId(), savedNode.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.RULE_CHAIN));
                int index = nodes.indexOf(node);
                nodes.set(index, savedNode);
                ruleNodeIndexMap.put(savedNode.getId(), index);
            }
        }
        if (!toDelete.isEmpty()) {
            deleteRuleNodes(tenantId, toDelete);
        }
        @Nullable RuleNodeId firstRuleNodeId = null;
        if (nodes != null) {
            if (ruleChainMetaData.getFirstNodeIndex() != null) {
                firstRuleNodeId = nodes.get(ruleChainMetaData.getFirstNodeIndex()).getId();
            }
            if ((ruleChain.getFirstRuleNodeId() != null && !ruleChain.getFirstRuleNodeId().equals(firstRuleNodeId))
                    || (ruleChain.getFirstRuleNodeId() == null && firstRuleNodeId != null)) {
                ruleChain.setFirstRuleNodeId(firstRuleNodeId);
                ruleChainDao.save(tenantId, ruleChain);
            }
            if (ruleChainMetaData.getConnections() != null) {
                for (@NotNull NodeConnectionInfo nodeConnection : ruleChainMetaData.getConnections()) {
                    EntityId from = nodes.get(nodeConnection.getFromIndex()).getId();
                    EntityId to = nodes.get(nodeConnection.getToIndex()).getId();
                    String type = nodeConnection.getType();
                    relations.add(new EntityRelation(from, to, type, RelationTypeGroup.RULE_NODE));
                }
            }
            if (ruleChainMetaData.getRuleChainConnections() != null) {
                for (@NotNull RuleChainConnectionInfo nodeToRuleChainConnection : ruleChainMetaData.getRuleChainConnections()) {
                    RuleChainId targetRuleChainId = nodeToRuleChainConnection.getTargetRuleChainId();
                    RuleChain targetRuleChain = findRuleChainById(TenantId.SYS_TENANT_ID, targetRuleChainId);
                    RuleNode targetNode = new RuleNode();
                    targetNode.setName(targetRuleChain != null ? targetRuleChain.getName() : "Rule Chain Input");
                    targetNode.setRuleChainId(ruleChain.getId());
                    targetNode.setType("org.echoiot.rule.engine.flow.TbRuleChainInputNode");
                    var configuration = JacksonUtil.newObjectNode();
                    configuration.put("ruleChainId", targetRuleChainId.getId().toString());
                    targetNode.setConfiguration(configuration);
                    ObjectNode layout = (ObjectNode) nodeToRuleChainConnection.getAdditionalInfo();
                    layout.remove("description");
                    layout.remove("ruleChainNodeId");
                    targetNode.setAdditionalInfo(layout);
                    targetNode.setDebugMode(false);
                    targetNode = ruleNodeDao.save(tenantId, targetNode);

                    @NotNull EntityRelation sourceRuleChainToRuleNode = new EntityRelation();
                    sourceRuleChainToRuleNode.setFrom(ruleChain.getId());
                    sourceRuleChainToRuleNode.setTo(targetNode.getId());
                    sourceRuleChainToRuleNode.setType(EntityRelation.CONTAINS_TYPE);
                    sourceRuleChainToRuleNode.setTypeGroup(RelationTypeGroup.RULE_CHAIN);
                    relations.add(sourceRuleChainToRuleNode);

                    @NotNull EntityRelation sourceRuleNodeToTargetRuleNode = new EntityRelation();
                    EntityId from = nodes.get(nodeToRuleChainConnection.getFromIndex()).getId();
                    sourceRuleNodeToTargetRuleNode.setFrom(from);
                    sourceRuleNodeToTargetRuleNode.setTo(targetNode.getId());
                    sourceRuleNodeToTargetRuleNode.setType(nodeToRuleChainConnection.getType());
                    sourceRuleNodeToTargetRuleNode.setTypeGroup(RelationTypeGroup.RULE_NODE);
                    relations.add(sourceRuleNodeToTargetRuleNode);
                }
            }
        }

        if (!relations.isEmpty()) {
            relationService.saveRelations(tenantId, relations);
        }

        return RuleChainUpdateResult.successful(updatedRuleNodes);
    }

    @Nullable
    @Override
    public RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id.");
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        if (ruleChain == null) {
            return null;
        }
        @NotNull RuleChainMetaData ruleChainMetaData = new RuleChainMetaData();
        ruleChainMetaData.setRuleChainId(ruleChainId);
        @NotNull List<RuleNode> ruleNodes = getRuleChainNodes(tenantId, ruleChainId);
        Collections.sort(ruleNodes, Comparator.comparingLong(RuleNode::getCreatedTime).thenComparing(RuleNode::getId, Comparator.comparing(RuleNodeId::getId)));
        @NotNull Map<RuleNodeId, Integer> ruleNodeIndexMap = new HashMap<>();
        for (@NotNull RuleNode node : ruleNodes) {
            ruleNodeIndexMap.put(node.getId(), ruleNodes.indexOf(node));
        }
        ruleChainMetaData.setNodes(ruleNodes);
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChainMetaData.setFirstNodeIndex(ruleNodeIndexMap.get(ruleChain.getFirstRuleNodeId()));
        }
        for (@NotNull RuleNode node : ruleNodes) {
            int fromIndex = ruleNodeIndexMap.get(node.getId());
            @NotNull List<EntityRelation> nodeRelations = getRuleNodeRelations(tenantId, node.getId());
            for (@NotNull EntityRelation nodeRelation : nodeRelations) {
                String type = nodeRelation.getType();
                if (nodeRelation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    @NotNull RuleNodeId toNodeId = new RuleNodeId(nodeRelation.getTo().getId());
                    int toIndex = ruleNodeIndexMap.get(toNodeId);
                    ruleChainMetaData.addConnectionInfo(fromIndex, toIndex, type);
                } else if (nodeRelation.getTo().getEntityType() == EntityType.RULE_CHAIN) {
                    log.warn("[{}][{}] Unsupported node relation: {}", tenantId, ruleChainId, nodeRelation.getTo());
                }
            }
        }
        if (ruleChainMetaData.getConnections() != null) {
            Collections.sort(ruleChainMetaData.getConnections(), Comparator.comparingInt(NodeConnectionInfo::getFromIndex)
                    .thenComparing(NodeConnectionInfo::getToIndex).thenComparing(NodeConnectionInfo::getType));
        }
        return ruleChainMetaData;
    }

    @Override
    public RuleChain findRuleChainById(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findById(tenantId, ruleChainId.getId());
    }

    @Override
    public RuleNode findRuleNodeById(TenantId tenantId, @NotNull RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findById(tenantId, ruleNodeId.getId());
    }

    @Override
    public ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        return ruleChainDao.findByIdAsync(tenantId, ruleChainId.getId());
    }

    @Override
    public ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, @NotNull RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        return ruleNodeDao.findByIdAsync(tenantId, ruleNodeId.getId());
    }

    @Override
    public RuleChain getRootTenantRuleChain(@NotNull TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        return ruleChainDao.findRootRuleChainByTenantIdAndType(tenantId.getId(), RuleChainType.CORE);
    }

    @NotNull
    @Override
    public List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        @NotNull List<RuleNode> ruleNodes = new ArrayList<>();
        for (@NotNull EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getTo().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            } else {
                relationService.deleteRelation(tenantId, relation);
            }
        }
        return ruleNodes;
    }

    @NotNull
    @Override
    public List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for search request.");
        List<EntityRelation> relations = getNodeToRuleChainRelations(tenantId, ruleChainId);
        @NotNull List<RuleNode> ruleNodes = new ArrayList<>();
        for (@NotNull EntityRelation relation : relations) {
            RuleNode ruleNode = ruleNodeDao.findById(tenantId, relation.getFrom().getId());
            if (ruleNode != null) {
                ruleNodes.add(ruleNode);
            }
        }
        return ruleNodes;
    }

    @NotNull
    @Override
    public List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId) {
        Validator.validateId(ruleNodeId, "Incorrect rule node id for search request.");
        List<EntityRelation> relations = relationService.findByFrom(tenantId, ruleNodeId, RelationTypeGroup.RULE_NODE);
        @NotNull List<EntityRelation> validRelations = new ArrayList<>();
        for (@NotNull EntityRelation relation : relations) {
            boolean valid = true;
            EntityType toType = relation.getTo().getEntityType();
            if (toType == EntityType.RULE_NODE || toType == EntityType.RULE_CHAIN) {
                BaseData<?> entity;
                if (relation.getTo().getEntityType() == EntityType.RULE_NODE) {
                    entity = ruleNodeDao.findById(tenantId, relation.getTo().getId());
                } else {
                    entity = ruleChainDao.findById(tenantId, relation.getTo().getId());
                }
                if (entity == null) {
                    relationService.deleteRelation(tenantId, relation);
                    valid = false;
                }
            }
            if (valid) {
                validRelations.add(relation);
            }
        }
        return validRelations;
    }

    @Override
    public PageData<RuleChain> findTenantRuleChainsByType(@NotNull TenantId tenantId, RuleChainType type, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        return ruleChainDao.findRuleChainsByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public Collection<RuleChain> findTenantRuleChainsByTypeAndName(TenantId tenantId, RuleChainType type, String name) {
        return ruleChainDao.findByTenantIdAndTypeAndName(tenantId, type, name);
    }

    @Override
    @Transactional
    public void deleteRuleChainById(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        Validator.validateId(ruleChainId, "Incorrect rule chain id for delete request.");
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        if (ruleChain != null) {
            if (ruleChain.isRoot()) {
                throw new DataValidationException("Deletion of Root Tenant Rule Chain is prohibited!");
            }
            if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
                PageData<Edge> pageData;
                do {
                    pageData = edgeService.findEdgesByTenantIdAndEntityId(tenantId, ruleChainId, pageLink);
                    if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                        for (@NotNull Edge edge : pageData.getData()) {
                            if (edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
                                throw new DataValidationException("Can't delete rule chain that is root for edge [" + edge.getName() + "]. Please assign another root rule chain first to the edge!");
                            }
                        }
                        if (pageData.hasNext()) {
                            pageLink = pageLink.nextPageLink();
                        }
                    }
                } while (pageData != null && pageData.hasNext());
            }
        }
        checkRuleNodesAndDelete(tenantId, ruleChainId);
    }

    @Override
    public void deleteRuleChainsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete rule chains request.");
        tenantRuleChainsRemover.removeEntities(tenantId, tenantId);
    }

    @NotNull
    @Override
    public RuleChainData exportTenantRuleChains(@NotNull TenantId tenantId, PageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search rule chain request.");
        Validator.validatePageLink(pageLink);
        PageData<RuleChain> ruleChainData = ruleChainDao.findRuleChainsByTenantId(tenantId.getId(), pageLink);
        List<RuleChain> ruleChains = ruleChainData.getData();
        @NotNull List<RuleChainMetaData> metadata = ruleChains.stream().map(rc -> loadRuleChainMetaData(tenantId, rc.getId())).collect(Collectors.toList());
        @NotNull RuleChainData rcData = new RuleChainData();
        rcData.setRuleChains(ruleChains);
        rcData.setMetadata(metadata);
        setRandomRuleChainIds(rcData);
        resetRuleNodeIds(metadata);
        return rcData;
    }

    @NotNull
    @Override
    public List<RuleChainImportResult> importTenantRuleChains(@NotNull TenantId tenantId, @NotNull RuleChainData ruleChainData, boolean overwrite) {
        @NotNull List<RuleChainImportResult> importResults = new ArrayList<>();

        setRandomRuleChainIds(ruleChainData);
        resetRuleNodeIds(ruleChainData.getMetadata());
        resetRuleChainMetadataTenantIds(tenantId, ruleChainData.getMetadata());

        for (@NotNull RuleChain ruleChain : ruleChainData.getRuleChains()) {
            @NotNull RuleChainImportResult importResult = new RuleChainImportResult();

            ruleChain.setTenantId(tenantId);
            ruleChain.setRoot(false);

            if (overwrite) {
                Collection<RuleChain> existingRuleChains = findTenantRuleChainsByTypeAndName(tenantId,
                        Optional.ofNullable(ruleChain.getType()).orElse(RuleChainType.CORE), ruleChain.getName());
                @NotNull Optional<RuleChain> existingRuleChain = existingRuleChains.stream().findFirst();
                if (existingRuleChain.isPresent()) {
                    setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), ruleChain.getId(), existingRuleChain.get().getId());
                    ruleChain.setRoot(existingRuleChain.get().isRoot());
                    importResult.setUpdated(true);
                }
            }

            try {
                ruleChain = saveRuleChain(ruleChain);
            } catch (Exception e) {
                importResult.setError(ExceptionUtils.getRootCauseMessage(e));
            }

            importResult.setTenantId(tenantId);
            importResult.setRuleChainId(ruleChain.getId());
            importResult.setRuleChainName(ruleChain.getName());
            importResults.add(importResult);
        }

        if (CollectionUtils.isNotEmpty(ruleChainData.getMetadata())) {
            ruleChainData.getMetadata().forEach(md -> saveRuleChainMetaData(tenantId, md));
        }

        return importResults;
    }

    private void resetRuleChainMetadataTenantIds(@NotNull TenantId tenantId, @NotNull List<RuleChainMetaData> metaData) {
        for (@NotNull RuleChainMetaData md : metaData) {
            for (@NotNull RuleNode node : md.getNodes()) {
                JsonNode nodeConfiguration = node.getConfiguration();
                searchTenantIdRecursive(tenantId, nodeConfiguration);
            }
        }
    }

    private void searchTenantIdRecursive(@NotNull TenantId tenantId, @NotNull JsonNode node) {
        Iterator<String> iter = node.fieldNames();
        boolean isTenantId = false;
        while (iter.hasNext()) {
            String field = iter.next();
            if ("entityType".equals(field) && TENANT.equals(node.get(field).asText())) {
                isTenantId = true;
                break;
            }
        }
        if (isTenantId) {
            @NotNull ObjectNode objNode = (ObjectNode) node;
            if (objNode.has("id")) {
                objNode.put("id", tenantId.getId().toString());
            }
        } else {
            for (@NotNull JsonNode jsonNode : node) {
                searchTenantIdRecursive(tenantId, jsonNode);
            }
        }
    }

    private void setRandomRuleChainIds(@NotNull RuleChainData ruleChainData) {
        for (@NotNull RuleChain ruleChain : ruleChainData.getRuleChains()) {
            RuleChainId oldRuleChainId = ruleChain.getId();
            @NotNull RuleChainId newRuleChainId = new RuleChainId(Uuids.timeBased());
            setNewRuleChainId(ruleChain, ruleChainData.getMetadata(), oldRuleChainId, newRuleChainId);
            ruleChain.setTenantId(null);
        }
    }

    private void resetRuleNodeIds(@NotNull List<RuleChainMetaData> metaData) {
        for (@NotNull RuleChainMetaData md : metaData) {
            for (@NotNull RuleNode node : md.getNodes()) {
                node.setId(null);
                node.setRuleChainId(null);
            }
        }
    }

    private void setNewRuleChainId(@NotNull RuleChain ruleChain, @NotNull List<RuleChainMetaData> metadata, @NotNull RuleChainId oldRuleChainId, @NotNull RuleChainId newRuleChainId) {
        ruleChain.setId(newRuleChainId);
        for (@NotNull RuleChainMetaData metaData : metadata) {
            if (metaData.getRuleChainId().equals(oldRuleChainId)) {
                metaData.setRuleChainId(newRuleChainId);
            }
            if (!CollectionUtils.isEmpty(metaData.getRuleChainConnections())) {
                for (@NotNull RuleChainConnectionInfo rcConnInfo : metaData.getRuleChainConnections()) {
                    if (rcConnInfo.getTargetRuleChainId().equals(oldRuleChainId)) {
                        rcConnInfo.setTargetRuleChainId(newRuleChainId);
                    }
                }
            }
            if (!CollectionUtils.isEmpty(metaData.getNodes())) {
                metaData.getNodes().stream()
                        .filter(ruleNode -> ruleNode.getType().equals(TB_RULE_CHAIN_INPUT_NODE))
                        .forEach(ruleNode -> {
                            ObjectNode configuration = (ObjectNode) ruleNode.getConfiguration();
                            if (configuration.has("ruleChainId")) {
                                if (configuration.get("ruleChainId").asText().equals(oldRuleChainId.toString())) {
                                    configuration.put("ruleChainId", newRuleChainId.toString());
                                    ruleNode.setConfiguration(configuration);
                                }
                            }
                        });
            }
        }
    }

    @NotNull
    @Override
    public RuleChain assignRuleChainToEdge(TenantId tenantId, @NotNull RuleChainId ruleChainId, EdgeId edgeId) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign ruleChain to non-existent edge!");
        }
        if (!edge.getTenantId().equals(ruleChain.getTenantId())) {
            throw new DataValidationException("Can't assign ruleChain to edge from different tenant!");
        }
        if (!RuleChainType.EDGE.equals(ruleChain.getType())) {
            throw new DataValidationException("Can't assign non EDGE ruleChain to edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create ruleChain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        return ruleChain;
    }

    @Override
    public RuleChain unassignRuleChainFromEdge(TenantId tenantId, @NotNull RuleChainId ruleChainId, EdgeId edgeId, boolean remove) {
        RuleChain ruleChain = findRuleChainById(tenantId, ruleChainId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign rule chain from non-existent edge!");
        }
        if (!remove && edge.getRootRuleChainId() != null && edge.getRootRuleChainId().equals(ruleChainId)) {
            throw new DataValidationException("Can't unassign root rule chain from edge [" + edge.getName() + "]. Please assign another root rule chain first!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, ruleChainId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete rule chain relation. Edge Id: [{}]", ruleChainId, edgeId);
            throw new RuntimeException(e);
        }
        return ruleChain;
    }

    @Override
    public PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(@NotNull TenantId tenantId, @NotNull EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findRuleChainsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validatePageLink(pageLink);
        return ruleChainDao.findRuleChainsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public RuleChain getEdgeTemplateRootRuleChain(@NotNull TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for search request.");
        return ruleChainDao.findRootRuleChainByTenantIdAndType(tenantId.getId(), RuleChainType.EDGE);
    }

    @Override
    public boolean setEdgeTemplateRootRuleChain(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        RuleChain ruleChain = ruleChainDao.findById(tenantId, ruleChainId.getId());
        RuleChain previousEdgeTemplateRootRuleChain = getEdgeTemplateRootRuleChain(ruleChain.getTenantId());
        if (previousEdgeTemplateRootRuleChain == null || !previousEdgeTemplateRootRuleChain.getId().equals(ruleChain.getId())) {
            try {
                if (previousEdgeTemplateRootRuleChain != null) {
                    previousEdgeTemplateRootRuleChain.setRoot(false);
                    ruleChainDao.save(tenantId, previousEdgeTemplateRootRuleChain);
                }
                ruleChain.setRoot(true);
                ruleChainDao.save(tenantId, ruleChain);
                return true;
            } catch (Exception e) {
                log.warn("Failed to set edge template root rule chain, ruleChainId: [{}]", ruleChainId, e);
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    public boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            createRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to set auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId) {
        try {
            deleteRelation(tenantId, new EntityRelation(tenantId, ruleChainId,
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE_AUTO_ASSIGN_RULE_CHAIN));
            return true;
        } catch (Exception e) {
            log.warn("Failed to unset auto assign to edge rule chain, ruleChainId: [{}]", ruleChainId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(@NotNull TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAutoAssignToEdgeRuleChainsByTenantId, tenantId [{}], pageLink {}", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return ruleChainDao.findAutoAssignToEdgeRuleChainsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String search) {
        log.trace("Executing findRuleNodes, tenantId [{}], type {}, search {}", tenantId, type, search);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type of the rule node");
        Validator.validateString(search, "Incorrect search text");
        return ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId, type, search);
    }

    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type) {
        log.trace("Executing findRuleNodes, tenantId [{}], type {}", tenantId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(type, "Incorrect type of the rule node");
        return ruleNodeDao.findRuleNodesByTenantIdAndType(tenantId, type, "");
    }

    @Override
    public PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink) {
        log.trace("Executing findAllRuleNodesByType, type {}, pageLink {}", type, pageLink);
        Validator.validateString(type, "Incorrect type of the rule node");
        Validator.validatePageLink(pageLink);
        return ruleNodeDao.findAllRuleNodesByType(type, pageLink);
    }

    @Override
    public RuleNode saveRuleNode(TenantId tenantId, RuleNode ruleNode) {
        return ruleNodeDao.save(tenantId, ruleNode);
    }

    private void checkRuleNodesAndDelete(TenantId tenantId, @NotNull RuleChainId ruleChainId) {
        try {
            ruleChainDao.removeById(tenantId, ruleChainId.getId());
        } catch (Exception t) {
            @Nullable ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_rule_chain_device_profile")) {
                throw new DataValidationException("The rule chain referenced by the device profiles cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_rule_chain_asset_profile")) {
                throw new DataValidationException("The rule chain referenced by the asset profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteRuleNodes(tenantId, ruleChainId);
    }

    private void deleteRuleNodes(TenantId tenantId, @NotNull List<RuleNode> ruleNodes) {
        @NotNull List<RuleNodeId> ruleNodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
        for (@NotNull var node : ruleNodes) {
            deleteEntityRelations(tenantId, node.getId());
        }
        ruleNodeDao.deleteByIdIn(ruleNodeIds);
    }

    @Override
    @Transactional
    public void deleteRuleNodes(TenantId tenantId, RuleChainId ruleChainId) {
        List<EntityRelation> nodeRelations = getRuleChainToNodeRelations(tenantId, ruleChainId);
        for (@NotNull EntityRelation relation : nodeRelations) {
            deleteRuleNode(tenantId, relation.getTo());
        }
        deleteEntityRelations(tenantId, ruleChainId);
    }

    private List<EntityRelation> getRuleChainToNodeRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByFrom(tenantId, ruleChainId, RelationTypeGroup.RULE_CHAIN);
    }

    private List<EntityRelation> getNodeToRuleChainRelations(TenantId tenantId, RuleChainId ruleChainId) {
        return relationService.findByTo(tenantId, ruleChainId, RelationTypeGroup.RULE_NODE);
    }

    private void deleteRuleNode(TenantId tenantId, @NotNull EntityId entityId) {
        deleteEntityRelations(tenantId, entityId);
        ruleNodeDao.removeById(tenantId, entityId.getId());
    }

    private final PaginatedRemover<TenantId, RuleChain> tenantRuleChainsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<RuleChain> findEntities(TenantId tenantId, @NotNull TenantId id, PageLink pageLink) {
                    return ruleChainDao.findRuleChainsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, @NotNull RuleChain entity) {
                    checkRuleNodesAndDelete(tenantId, entity.getId());
                }
            };

}
