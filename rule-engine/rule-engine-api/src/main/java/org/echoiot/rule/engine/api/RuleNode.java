package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.plugin.ComponentScope;
import org.echoiot.server.common.data.plugin.ComponentType;
import org.echoiot.server.common.data.rule.RuleChainType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 规则引擎节点定义注解
 * 这个注解用于标记注册引擎节点，并且提供节点的基本信息
 *
 * @author Echo
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleNode {

    /**
     * 节点类型
     */
    ComponentType type();

    /**
     * 节点名称
     */
    String name();

    /**
     * 节点描述
     */
    String nodeDescription();

    /**
     * 节点详细信息
     */
    String nodeDetails();

    /**
     * 节点配置类
     */
    Class<? extends NodeConfiguration> configClazz();

    /**
     * TODO 释义未知
     */
    boolean inEnabled() default true;

    /**
     * TODO 释义未知
     */
    boolean outEnabled() default true;

    /**
     * 节点作用域
     */
    ComponentScope scope() default ComponentScope.TENANT;

    /**
     * 关系类型
     */
    String[] relationTypes() default {"Success", "Failure"};

    /**
     * 节点UI界面资源
     */
    String[] uiResources() default {};

    /**
     * 节点配置指令
     */
    String configDirective() default "";

    /**
     * 节点图标
     */
    String icon() default "";

    /**
     * 节点图标URL
     */
    String iconUrl() default "";

    /**
     * 节点文档URL
     */
    String docUrl() default "";

    /**
     * 是否自定义关系
     */
    boolean customRelations() default false;

    /**
     * 是否是规则链节点
     */
    boolean ruleChainNode() default false;

    /**
     * 规则链类型
     */
    RuleChainType[] ruleChainTypes() default {RuleChainType.CORE, RuleChainType.EDGE};

}
