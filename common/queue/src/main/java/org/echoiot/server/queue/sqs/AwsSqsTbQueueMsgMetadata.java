package org.echoiot.server.queue.sqs;

import com.amazonaws.http.SdkHttpMetadata;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.echoiot.server.queue.TbQueueMsgMetadata;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class AwsSqsTbQueueMsgMetadata implements TbQueueMsgMetadata {

    @NotNull
    private final SdkHttpMetadata metadata;
}
