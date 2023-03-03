package org.echoiot.rule.engine.api;

/**
 * 定义节点配置接口
 *
 * @author Echo
 */
public interface NodeConfiguration {

    /**
     * 获取默认配置
     *
     * @return 默认配置
     */
    NodeConfiguration defaultConfiguration();

}
