package org.echoiot.server.cache;

import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnMissingBean(TbCaffeineCacheConfiguration.class)
@ConditionalOnProperty(prefix = "redis.connection", value = "type", havingValue = "cluster")
public class TBRedisClusterConfiguration extends TBRedisCacheConfiguration {

    private static final String COMMA = ",";
    private static final String COLON = ":";

    @Value("${redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${redis.cluster.max-redirects:12}")
    private Integer maxRedirects;

    @Value("${redis.cluster.useDefaultPoolConfig:true}")
    private boolean useDefaultPoolConfig;

    @Value("${redis.password:}")
    private String password;

    @NotNull
    public JedisConnectionFactory loadFactory() {
        @NotNull RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration();
        clusterConfiguration.setClusterNodes(getNodes(clusterNodes));
        clusterConfiguration.setMaxRedirects(maxRedirects);
        clusterConfiguration.setPassword(password);
        if (useDefaultPoolConfig) {
            return new JedisConnectionFactory(clusterConfiguration);
        } else {
            return new JedisConnectionFactory(clusterConfiguration, buildPoolConfig());
        }
    }

    @NotNull
    private List<RedisNode> getNodes(@NotNull String nodes) {
        List<RedisNode> result;
        if (StringUtils.isBlank(nodes)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (@NotNull String hostPort : nodes.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                @NotNull Integer port = Integer.valueOf(hostPort.split(COLON)[1]);
                result.add(new RedisNode(host, port));
            }
        }
        return result;
    }
}
