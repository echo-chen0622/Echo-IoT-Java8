package org.echoiot.server.service.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.echoiot.rule.engine.api.NodeDefinition;
import org.echoiot.rule.engine.api.RuleNode;
import org.echoiot.rule.engine.api.TbRelationTypes;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.plugin.ComponentDescriptor;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.rule.RuleChainType;
import org.echoiot.server.dao.component.ComponentDescriptorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

import static org.echoiot.common.util.SpringUtils.getBeanDefinitions;

/**
 * 注释组件发现服务
 */
@Service
@Slf4j
public class AnnotationComponentDiscoveryService implements ComponentDiscoveryService {

    /**
     * 最大优化重试次数
     */
    public static final int MAX_OPTIMISITC_RETRIES = 3;

    /**
     * 扫描规则引擎组件的包路径
     */
    @Value("${plugins.scan_packages}")
    private String[] scanPackages;

    @Resource
    private Environment environment;

    @Resource
    private ComponentDescriptorService componentDescriptorService;

    private final Map<String, ComponentDescriptor> components = new HashMap<>();

    /**
     * 核心组件
     */
    private final EnumMap<ComponentType, List<ComponentDescriptor>> coreComponentsMap = new EnumMap<>(ComponentType.class);

    /**
     * 边缘组件
     */
    private final EnumMap<ComponentType, List<ComponentDescriptor>> edgeComponentsMap = new EnumMap<>(ComponentType.class);

    /**
     * 这里写法比较老，应该用 spring boot 统一的序列化工具，以此保证序列化能力的一致性。而不是 new 一个。
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 是否是安装
     */
    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    /**
     * 初始化类
     */
    @PostConstruct
    public void init() {
        if (!isInstall()) {
            // 如果不是执行安装功能，就执行生成规则引擎组件Bean。安装的话，因为还没创建数据库，不能启动时就发现组件，会发现不到
            discoverComponents();
        }
    }

    /**
     * 扫描并注册规则节点组件
     */
    private void registerRuleNodeComponents() {
        // 获取规则节点注解的bean定义
        Set<BeanDefinition> ruleNodeBeanDefinitions = getBeanDefinitions(RuleNode.class, scanPackages);
        // 遍历bean定义
        for (BeanDefinition def : ruleNodeBeanDefinitions) {
            int retryCount = 0;
            @Nullable Exception cause = null;
            // 未免特殊意外导致的组件初始化失败，这里做了一个重试机制，每个节点最多重试3次
            while (retryCount < MAX_OPTIMISITC_RETRIES) {
                try {
                    // 获取组件类
                    Class<?> clazz = Class.forName(def.getBeanClassName());
                    // 获取规则节点注解
                    RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
                    // 获取组件类型
                    ComponentType type = ruleNodeAnnotation.type();
                    // 扫描并持久化组件
                    ComponentDescriptor component = scanAndPersistComponent(def, type);
                    // 将组件放入组件集合中
                    components.put(component.getClazz(), component);
                    // 将组件放入核心组件集合中
                    putComponentIntoMaps(type, ruleNodeAnnotation, component);
                    // 完成扫描与注册，退出循环，不再重试
                    break;
                } catch (Exception e) {
                    log.trace("无法初始化组件 {}, 因为 {}", def.getBeanClassName(), e.getMessage(), e);
                    cause = e;
                    // 重试次数加1
                    retryCount++;
                    try {
                        // 休眠1秒，再重试
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {//NOSONAR - 中断异常预计不会抛出在这里
                        // 休眠被打断，直接抛出异常
                        throw new RuntimeException(e1);
                    }
                }
            }
            // 如果重试次数达到最大重试次数，还是无法初始化组件，就抛出异常
            if (cause != null && retryCount == MAX_OPTIMISITC_RETRIES) {
                log.error("无法初始化组件 {}, 因为 {}", def.getBeanClassName(), cause.getMessage(), cause);
                throw new RuntimeException(cause);
            }
        }
    }

    /**
     * 将组件信息存到 map 中，作为本地缓存
     *
     * @param type
     * @param ruleNodeAnnotation
     * @param component
     */
    private void putComponentIntoMaps(ComponentType type, RuleNode ruleNodeAnnotation, ComponentDescriptor component) {
        // 根据组件类型，将组件放入核心组件集合中
        if (ruleChainTypeContainsArray(RuleChainType.CORE, ruleNodeAnnotation.ruleChainTypes())) {
            coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
        // 根据组件类型，将组件放入边缘组件集合中
        if (ruleChainTypeContainsArray(RuleChainType.EDGE, ruleNodeAnnotation.ruleChainTypes())) {
            edgeComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
    }

    /**
     * 检查规则链类型是否包含在数组中
     *
     * @param ruleChainType 规则链类型
     * @param array         规则链类型数组
     */
    private boolean ruleChainTypeContainsArray(RuleChainType ruleChainType, RuleChainType[] array) {
        for (RuleChainType tmp : array) {
            if (ruleChainType.equals(tmp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 扫描并持久化组件
     *
     * @param def  bean定义
     * @param type 组件类型
     */
    private ComponentDescriptor scanAndPersistComponent(@NotNull BeanDefinition def, ComponentType type) {
        // 创建组件描述
        ComponentDescriptor scannedComponent = new ComponentDescriptor();
        @Nullable String clazzName = def.getBeanClassName();
        try {
            // 设置组件类型
            scannedComponent.setType(type);
            Class<?> clazz = Class.forName(clazzName);
            RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
            // 设置组件名称
            scannedComponent.setName(ruleNodeAnnotation.name());
            // 设置组件适用范围
            scannedComponent.setScope(ruleNodeAnnotation.scope());
            // 设置组件配置与描述信息。这里冗余了 type 等基础信息，是为了方便前端展示
            NodeDefinition nodeDefinition = prepareNodeDefinition(ruleNodeAnnotation);
            JsonNode node = mapper.valueToTree(nodeDefinition);
            ObjectNode configurationDescriptor = mapper.createObjectNode();
            configurationDescriptor.set("nodeDefinition", node);
            scannedComponent.setConfigurationDescriptor(configurationDescriptor);
            // 设置组件类名
            scannedComponent.setClazz(clazzName);
            log.debug("组件扫描完成: {}", scannedComponent);
        } catch (Exception e) {
            log.error("无法初始化组件 {}, 错误原因： {}", def.getBeanClassName(), e.getMessage(), e);
            // 如果扫描组件失败，就抛出异常，直接退出
            throw new RuntimeException(e);
        }
        // 从数据库中获取已经持久化的组件
        ComponentDescriptor persistedComponent = componentDescriptorService.findByClazz(TenantId.SYS_TENANT_ID, clazzName);
        if (persistedComponent == null) {
            // 未发现老组件，直接保存
            log.debug("保存新组件: {}", scannedComponent);
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        } else if (scannedComponent.equals(persistedComponent)) {
            //组件已存在，且配置未变更，不需要更新
            log.debug("组件已持久化: {}", persistedComponent);
            scannedComponent = persistedComponent;
        } else {
            // 组件已存在，但配置发生变更，需要更新
            log.debug("组件 {} 将更新为 {}", persistedComponent, scannedComponent);
            // 删除老组件
            componentDescriptorService.deleteByClazz(TenantId.SYS_TENANT_ID, persistedComponent.getClazz());
            scannedComponent.setId(persistedComponent.getId());
            // 保存新组件
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        }
        // 返回扫描并持久化的组件
        return scannedComponent;
    }

    /**
     * 准备节点定义
     *
     * @param nodeAnnotation
     *
     * @throws Exception
     */
    private @NotNull NodeDefinition prepareNodeDefinition(@NotNull RuleNode nodeAnnotation) throws Exception {
        NodeDefinition nodeDefinition = new NodeDefinition();
        // 设置节点详情
        nodeDefinition.setDetails(nodeAnnotation.nodeDetails());
        // 设置节点描述
        nodeDefinition.setDescription(nodeAnnotation.nodeDescription());
        nodeDefinition.setInEnabled(nodeAnnotation.inEnabled());
        nodeDefinition.setOutEnabled(nodeAnnotation.outEnabled());
        // 设置节点类型
        nodeDefinition.setRelationTypes(getRelationTypesWithFailureRelation(nodeAnnotation));
        nodeDefinition.setCustomRelations(nodeAnnotation.customRelations());
        nodeDefinition.setRuleChainNode(nodeAnnotation.ruleChainNode());
        // 设置默认节点配置
        Class<? extends NodeConfiguration> configClazz = nodeAnnotation.configClazz();
        NodeConfiguration config = configClazz.getDeclaredConstructor().newInstance();
        NodeConfiguration defaultConfiguration = config.defaultConfiguration();
        // 利用ObjectMapper将NodeConfiguration转换为树结构的JsonNode
        nodeDefinition.setDefaultConfiguration(mapper.valueToTree(defaultConfiguration));
        // 设置节点UI资源
        nodeDefinition.setUiResources(nodeAnnotation.uiResources());
        // 设置节点配置指令
        nodeDefinition.setConfigDirective(nodeAnnotation.configDirective());
        // 设置节点图标
        nodeDefinition.setIcon(nodeAnnotation.icon());
        // 设置节点图标URL
        nodeDefinition.setIconUrl(nodeAnnotation.iconUrl());
        // 设置节点文档URL
        nodeDefinition.setDocUrl(nodeAnnotation.docUrl());
        return nodeDefinition;
    }

    /**
     * 获取关系类型，如果没有FAILURE关系类型，则添加FAILURE关系类型
     *
     * @param nodeAnnotation
     */
    private String @NotNull [] getRelationTypesWithFailureRelation(RuleNode nodeAnnotation) {
        List<String> relationTypes = new ArrayList<>(Arrays.asList(nodeAnnotation.relationTypes()));
        if (!relationTypes.contains(TbRelationTypes.FAILURE)) {
            relationTypes.add(TbRelationTypes.FAILURE);
        }
        return relationTypes.toArray(new String[0]);
    }


    /**
     * 生成规则引擎组件Bean
     */
    @Override
    public void discoverComponents() {
        // 扫描并注册规则节点组件
        registerRuleNodeComponents();
        log.debug("找到定义: {}", components.values());
    }

    /**
     * 获取组件
     *
     * @param type
     * @param ruleChainType
     */
    @Override
    public List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            // 核心组件
            if (coreComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(coreComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            // 边缘组件
            if (edgeComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(edgeComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else {
            log.error("不支持的规则链类型 {}", ruleChainType);
            throw new RuntimeException("不支持的规则链类型 " + ruleChainType);
        }
    }

    /**
     * 获取组件
     *
     * @param types         组件类型
     * @param ruleChainType 规则链类型
     */
    @Override
    public List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            // 核心组件 这里写法感觉有点过度设计了，没必要一顿抽象
            return getComponents(types, coreComponentsMap);
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            // 边缘组件
            return getComponents(types, edgeComponentsMap);
        } else {
            log.error("不支持的规则链类型 {}", ruleChainType);
            throw new RuntimeException("不支持的规则链类型 " + ruleChainType);
        }
    }

    /**
     * 获取组件
     *
     * @param clazz 组件类名
     */
    @Override
    public Optional<ComponentDescriptor> getComponent(String clazz) {
        return Optional.ofNullable(components.get(clazz));
    }

    /**
     * 获取组件
     *
     * @param types         组件类型
     * @param componentsMap 组件Map
     */
    private List<ComponentDescriptor> getComponents(Set<ComponentType> types, Map<ComponentType, List<ComponentDescriptor>> componentsMap) {
        List<ComponentDescriptor> result = new ArrayList<>();
        types.stream().filter(componentsMap::containsKey).forEach(type -> result.addAll(componentsMap.get(type)));
        return Collections.unmodifiableList(result);
    }
}
