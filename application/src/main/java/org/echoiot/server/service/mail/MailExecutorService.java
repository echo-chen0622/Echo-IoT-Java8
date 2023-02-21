package org.echoiot.server.service.mail;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MailExecutorService extends AbstractListeningExecutor {

    @Value("${actors.rule.mail_thread_pool_size}")
    private int mailExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return mailExecutorThreadPoolSize;
    }

}
