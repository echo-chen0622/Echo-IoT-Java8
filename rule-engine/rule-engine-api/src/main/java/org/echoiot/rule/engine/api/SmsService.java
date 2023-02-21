package org.echoiot.rule.engine.api;

import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.sms.config.TestSmsRequest;

public interface SmsService {

    void updateSmsConfiguration();

    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws EchoiotException;

    void sendTestSms(TestSmsRequest testSmsRequest) throws EchoiotException;

    boolean isConfigured(TenantId tenantId);

}
