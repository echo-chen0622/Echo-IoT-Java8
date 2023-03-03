package org.echoiot.common.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * spring 相关工具类
 */
public class SpringUtils extends SpringUtil {

    /**
     * 配置文件名称，pom 文件配置了 spring.config.name 的值为 echoiot
     */
    public static final String SPRING_CONFIG_NAME_KEY = "--spring.config.name";

    /**
     * 主要用来设置配置文件名称
     *
     * @param args
     */
    public static String[] updateArguments(String[] args, String defaultSpringConfigParam) {
        // 如果没有设置配置文件名称，则设置默认的配置文件名称
        if (Arrays.stream(args).noneMatch(arg -> arg.startsWith(SPRING_CONFIG_NAME_KEY))) {
            // 使数组长度加 1，然后把配置文件名称放到最后一个位置
            String[] modifiedArgs = ArrayUtil.resize(args, args.length + 1);
            modifiedArgs[args.length] = defaultSpringConfigParam;
            return modifiedArgs;
        }
        return args;
    }


    /**
     * 扫描包，获取实现了特定注解的Bean定义
     *
     * @param componentType 注解类型 例如：@Component
     * @param scanPackages  扫描包路径，可以多个，例如：{"org.echoiot.server.service.install", "org.echoiot.server.service.install.update"}，这里必须指定包路径，不做全盘扫描
     */
    public static @NotNull Set<BeanDefinition> getBeanDefinitions(Class<? extends Annotation> componentType, String @NotNull [] scanPackages) {
        // 扫描器
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(componentType));
        Set<BeanDefinition> defs = new HashSet<>();
        //这里，必须指定包路径，不做全盘扫描
        for (String scanPackage : scanPackages) {
            defs.addAll(scanner.findCandidateComponents(scanPackage));
        }
        return defs;
    }
}
