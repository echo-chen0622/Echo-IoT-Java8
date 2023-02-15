package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RuleNode {

    ComponentType type();

    String name();

    String nodeDescription();

    String nodeDetails();

    Class<? extends NodeConfiguration> configClazz();

    boolean inEnabled() default true;

    boolean outEnabled() default true;

    ComponentScope scope() default ComponentScope.TENANT;

    String[] relationTypes() default {"Success", "Failure"};

    String[] uiResources() default {};

    String configDirective() default "";

    String icon() default "";

    String iconUrl() default "";

    String docUrl() default "";

    boolean customRelations() default false;

    boolean ruleChainNode() default false;

    RuleChainType[] ruleChainTypes() default {RuleChainType.CORE, RuleChainType.EDGE};

}
