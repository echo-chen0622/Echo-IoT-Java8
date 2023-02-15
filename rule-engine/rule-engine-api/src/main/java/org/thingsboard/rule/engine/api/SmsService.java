package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.sms.config.TestSmsRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;

public interface SmsService {

    void updateSmsConfiguration();

    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws ThingsboardException;;

    void sendTestSms(TestSmsRequest testSmsRequest) throws ThingsboardException;

    boolean isConfigured(TenantId tenantId);

}
