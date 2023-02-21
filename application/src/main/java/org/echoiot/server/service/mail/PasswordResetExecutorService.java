package org.echoiot.server.service.mail;

import org.echoiot.common.util.AbstractListeningExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetExecutorService extends AbstractListeningExecutor {

    @Value("${actors.rule.mail_password_reset_thread_pool_size:10}")
    private int mailExecutorThreadPoolSize;

    @Override
    protected int getThreadPollSize() {
        return mailExecutorThreadPoolSize;
    }

}
