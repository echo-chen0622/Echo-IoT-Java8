package org.echoiot.rule.engine.aws.sns;

import lombok.Data;
import org.echoiot.rule.engine.api.NodeConfiguration;
import org.jetbrains.annotations.NotNull;

@Data
public class TbSnsNodeConfiguration implements NodeConfiguration<TbSnsNodeConfiguration> {

    private String topicArnPattern;
    private String accessKeyId;
    private String secretAccessKey;
    private String region;

    @NotNull
    @Override
    public TbSnsNodeConfiguration defaultConfiguration() {
        @NotNull TbSnsNodeConfiguration configuration = new TbSnsNodeConfiguration();
        configuration.setTopicArnPattern("arn:aws:sns:us-east-1:123456789012:MyNewTopic");
        configuration.setRegion("us-east-1");
        return configuration;
    }
}
