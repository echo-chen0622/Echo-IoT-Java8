package org.echoiot.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.echoiot.server.common.data.ApiFeature;
import org.echoiot.server.common.data.ApiUsageStateMailMessage;
import org.echoiot.server.common.data.ApiUsageStateValue;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.springframework.mail.javamail.JavaMailSender;

public interface MailService {

    void updateMailConfiguration();

    void sendEmail(TenantId tenantId, String email, String subject, String message) throws EchoiotException;

    void sendTestMail(JsonNode config, String email) throws EchoiotException;

    void sendActivationEmail(String activationLink, String email) throws EchoiotException;

    void sendAccountActivatedEmail(String loginLink, String email) throws EchoiotException;

    void sendResetPasswordEmail(String passwordResetLink, String email) throws EchoiotException;

    void sendResetPasswordEmailAsync(String passwordResetLink, String email);

    void sendPasswordWasResetEmail(String loginLink, String email) throws EchoiotException;

    void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws EchoiotException;

    void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws EchoiotException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws EchoiotException;
    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws EchoiotException;

    void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageStateMailMessage msg) throws EchoiotException;

    void testConnection(TenantId tenantId) throws Exception;

}
