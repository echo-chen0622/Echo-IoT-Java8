package org.echoiot.common.util;

import cn.hutool.core.util.ArrayUtil;

import java.util.Arrays;

/**
 * spring 相关工具类
 */
public class ApplicationUtil {

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
}
