package org.thingsboard.server.queue.sqs;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
@Component
@Data
public class TbAwsSqsSettings {

    @Value("${queue.aws_sqs.use_default_credential_provider_chain}")
    private Boolean useDefaultCredentialProviderChain;

    @Value("${queue.aws_sqs.access_key_id}")
    private String accessKeyId;

    @Value("${queue.aws_sqs.secret_access_key}")
    private String secretAccessKey;

    @Value("${queue.aws_sqs.region}")
    private String region;

    @Value("${queue.aws_sqs.threads_per_topic}")
    private int threadsPerTopic;

}
