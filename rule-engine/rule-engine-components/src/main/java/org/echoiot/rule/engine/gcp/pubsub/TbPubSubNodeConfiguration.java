package org.echoiot.rule.engine.gcp.pubsub;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

@Data
public class TbPubSubNodeConfiguration implements NodeConfiguration<TbPubSubNodeConfiguration> {

    private String projectId;
    private String topicName;
    private Map<String, String> messageAttributes;
    private String serviceAccountKey;
    private String serviceAccountKeyFileName;

    @NotNull
    @Override
    public TbPubSubNodeConfiguration defaultConfiguration() {
        @NotNull TbPubSubNodeConfiguration configuration = new TbPubSubNodeConfiguration();
        configuration.setProjectId("my-google-cloud-project-id");
        configuration.setTopicName("my-pubsub-topic-name");
        configuration.setMessageAttributes(Collections.emptyMap());
        return configuration;
    }
}
