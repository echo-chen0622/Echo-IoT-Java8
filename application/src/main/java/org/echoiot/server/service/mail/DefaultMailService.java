package org.echoiot.server.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.echoiot.rule.engine.api.MailService;
import org.echoiot.rule.engine.api.TbEmail;
import org.echoiot.server.common.data.*;
import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.stats.TbApiUsageReportClient;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.service.apiusage.TbApiUsageStateService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.NestedRuntimeException;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class DefaultMailService implements MailService {

    public static final String MAIL_PROP = "mail.";
    public static final String TARGET_EMAIL = "targetEmail";
    public static final String UTF_8 = "UTF-8";
    public static final int _10K = 10000;
    public static final int _1M = 1000000;

    private final MessageSource messages;
    private final Configuration freemarkerConfig;
    private final AdminSettingsService adminSettingsService;
    private final TbApiUsageReportClient apiUsageClient;

    private static final long DEFAULT_TIMEOUT = 10_000;

    @Lazy
    @Resource
    private TbApiUsageStateService apiUsageStateService;

    @Resource
    private MailExecutorService mailExecutorService;

    @Resource
    private PasswordResetExecutorService passwordResetExecutorService;

    private JavaMailSenderImpl mailSender;

    private String mailFrom;

    private long timeout;

    public DefaultMailService(MessageSource messages, Configuration freemarkerConfig, AdminSettingsService adminSettingsService, TbApiUsageReportClient apiUsageClient) {
        this.messages = messages;
        this.freemarkerConfig = freemarkerConfig;
        this.adminSettingsService = adminSettingsService;
        this.apiUsageClient = apiUsageClient;
    }

    @PostConstruct
    private void init() {
        updateMailConfiguration();
    }

    @Override
    public void updateMailConfiguration() {
        AdminSettings settings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        if (settings != null) {
            JsonNode jsonConfig = settings.getJsonValue();
            mailSender = createMailSender(jsonConfig);
            mailFrom = jsonConfig.get("mailFrom").asText();
            timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);
        } else {
            throw new IncorrectParameterException("Failed to update mail configuration. Settings not found!");
        }
    }

    @NotNull
    private JavaMailSenderImpl createMailSender(@NotNull JsonNode jsonConfig) {
        @NotNull JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(jsonConfig.get("smtpHost").asText());
        mailSender.setPort(parsePort(jsonConfig.get("smtpPort").asText()));
        mailSender.setUsername(jsonConfig.get("username").asText());
        mailSender.setPassword(jsonConfig.get("password").asText());
        mailSender.setJavaMailProperties(createJavaMailProperties(jsonConfig));
        return mailSender;
    }

    @NotNull
    private Properties createJavaMailProperties(@NotNull JsonNode jsonConfig) {
        @NotNull Properties javaMailProperties = new Properties();
        String protocol = jsonConfig.get("smtpProtocol").asText();
        javaMailProperties.put("mail.transport.protocol", protocol);
        javaMailProperties.put(MAIL_PROP + protocol + ".host", jsonConfig.get("smtpHost").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".port", jsonConfig.get("smtpPort").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".timeout", jsonConfig.get("timeout").asText());
        javaMailProperties.put(MAIL_PROP + protocol + ".auth", String.valueOf(StringUtils.isNotEmpty(jsonConfig.get("username").asText())));
        boolean enableTls = false;
        if (jsonConfig.has("enableTls")) {
            if (jsonConfig.get("enableTls").isBoolean() && jsonConfig.get("enableTls").booleanValue()) {
                enableTls = true;
            } else if (jsonConfig.get("enableTls").isTextual()) {
                enableTls = "true".equalsIgnoreCase(jsonConfig.get("enableTls").asText());
            }
        }
        javaMailProperties.put(MAIL_PROP + protocol + ".starttls.enable", enableTls);
        if (enableTls && jsonConfig.has("tlsVersion") && !jsonConfig.get("tlsVersion").isNull()) {
            String tlsVersion = jsonConfig.get("tlsVersion").asText();
            if (StringUtils.isNoneEmpty(tlsVersion)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".ssl.protocols", tlsVersion);
            }
        }

        boolean enableProxy = jsonConfig.has("enableProxy") && jsonConfig.get("enableProxy").asBoolean();

        if (enableProxy) {
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.host", jsonConfig.get("proxyHost").asText());
            javaMailProperties.put(MAIL_PROP + protocol + ".proxy.port", jsonConfig.get("proxyPort").asText());
            String proxyUser = jsonConfig.get("proxyUser").asText();
            if (StringUtils.isNoneEmpty(proxyUser)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.user", proxyUser);
            }
            String proxyPassword = jsonConfig.get("proxyPassword").asText();
            if (StringUtils.isNoneEmpty(proxyPassword)) {
                javaMailProperties.put(MAIL_PROP + protocol + ".proxy.password", proxyPassword);
            }
        }
        return javaMailProperties;
    }

    private int parsePort(@NotNull String strPort) {
        try {
            return Integer.valueOf(strPort);
        } catch (NumberFormatException e) {
            throw new IncorrectParameterException(String.format("Invalid smtp port value: %s", strPort));
        }
    }

    @Override
    public void sendEmail(TenantId tenantId, @NotNull String email, @NotNull String subject, @NotNull String message) throws EchoiotException {
        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendTestMail(@NotNull JsonNode jsonConfig, @NotNull String email) throws EchoiotException {
        @NotNull JavaMailSenderImpl testMailSender = createMailSender(jsonConfig);
        String mailFrom = jsonConfig.get("mailFrom").asText();
        @NotNull String subject = messages.getMessage("test.message.subject", null, Locale.US);
        long timeout = jsonConfig.get("timeout").asLong(DEFAULT_TIMEOUT);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("test.ftl", model);

        sendMail(testMailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendActivationEmail(String activationLink, @NotNull String email) throws EchoiotException {

        @NotNull String subject = messages.getMessage("activation.subject", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("activationLink", activationLink);
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("activation.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendAccountActivatedEmail(String loginLink, @NotNull String email) throws EchoiotException {

        @NotNull String subject = messages.getMessage("account.activated.subject", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("account.activated.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmail(String passwordResetLink, @NotNull String email) throws EchoiotException {

        @NotNull String subject = messages.getMessage("reset.password.subject", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("passwordResetLink", passwordResetLink);
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("reset.password.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendResetPasswordEmailAsync(String passwordResetLink, @NotNull String email) {
        passwordResetExecutorService.execute(() -> {
            try {
                this.sendResetPasswordEmail(passwordResetLink, email);
            } catch (EchoiotException e) {
                log.error("Error occurred: {} ", e.getMessage());
            }
        });
    }

    @Override
    public void sendPasswordWasResetEmail(String loginLink, @NotNull String email) throws EchoiotException {

        @NotNull String subject = messages.getMessage("password.was.reset.subject", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("loginLink", loginLink);
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("password.was.reset.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, @NotNull TbEmail tbEmail) throws EchoiotException {
        sendMail(tenantId, customerId, tbEmail, this.mailSender, timeout);
    }

    @Override
    public void send(TenantId tenantId, CustomerId customerId, @NotNull TbEmail tbEmail, @NotNull JavaMailSender javaMailSender, long timeout) throws EchoiotException {
        sendMail(tenantId, customerId, tbEmail, javaMailSender, timeout);
    }

    private void sendMail(TenantId tenantId, CustomerId customerId, @NotNull TbEmail tbEmail, @NotNull JavaMailSender javaMailSender, long timeout) throws EchoiotException {
        if (apiUsageStateService.getApiUsageState(tenantId).isEmailSendEnabled()) {
            try {
                @NotNull MimeMessage mailMsg = javaMailSender.createMimeMessage();
                boolean multipart = (tbEmail.getImages() != null && !tbEmail.getImages().isEmpty());
                @NotNull MimeMessageHelper helper = new MimeMessageHelper(mailMsg, multipart, "UTF-8");
                helper.setFrom(StringUtils.isBlank(tbEmail.getFrom()) ? mailFrom : tbEmail.getFrom());
                helper.setTo(tbEmail.getTo().split("\\s*,\\s*"));
                if (!StringUtils.isBlank(tbEmail.getCc())) {
                    helper.setCc(tbEmail.getCc().split("\\s*,\\s*"));
                }
                if (!StringUtils.isBlank(tbEmail.getBcc())) {
                    helper.setBcc(tbEmail.getBcc().split("\\s*,\\s*"));
                }
                helper.setSubject(tbEmail.getSubject());
                helper.setText(tbEmail.getBody(), tbEmail.isHtml());

                if (multipart) {
                    for (@NotNull String imgId : tbEmail.getImages().keySet()) {
                        String imgValue = tbEmail.getImages().get(imgId);
                        @NotNull String value = imgValue.replaceFirst("^data:image/[^;]*;base64,?", "");
                        byte[] bytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(value);
                        String contentType = helper.getFileTypeMap().getContentType(imgId);
                        @NotNull InputStreamSource iss = () -> new ByteArrayInputStream(bytes);
                        helper.addInline(imgId, iss, contentType);
                    }
                }
                sendMailWithTimeout(javaMailSender, helper.getMimeMessage(), timeout);
                apiUsageClient.report(tenantId, customerId, ApiUsageRecordKey.EMAIL_EXEC_COUNT, 1);
            } catch (Exception e) {
                throw handleException(e);
            }
        } else {
            throw new RuntimeException("Email sending is disabled due to API limits!");
        }
    }

    @Override
    public void sendAccountLockoutEmail(String lockoutEmail, @NotNull String email, Integer maxFailedLoginAttempts) throws EchoiotException {
        @NotNull String subject = messages.getMessage("account.lockout.subject", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("lockoutAccount", lockoutEmail);
        model.put("maxFailedLoginAttempts", maxFailedLoginAttempts);
        model.put(TARGET_EMAIL, email);

        @NotNull String message = mergeTemplateIntoString("account.lockout.ftl", model);

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendTwoFaVerificationEmail(@NotNull String email, @NotNull String verificationCode, int expirationTimeSeconds) throws EchoiotException {
        @NotNull String subject = messages.getMessage("2fa.verification.code.subject", null, Locale.US);
        @NotNull String message = mergeTemplateIntoString("2fa.verification.code.ftl", Map.of(
                TARGET_EMAIL, email,
                "code", verificationCode,
                "expirationTimeSeconds", expirationTimeSeconds
                                                                                             ));

        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void sendApiFeatureStateEmail(@NotNull ApiFeature apiFeature, @NotNull ApiUsageStateValue stateValue, @NotNull String email, @NotNull ApiUsageStateMailMessage msg) throws EchoiotException {
        @NotNull String subject = messages.getMessage("api.usage.state", null, Locale.US);

        @NotNull Map<String, Object> model = new HashMap<>();
        model.put("apiFeature", apiFeature.getLabel());
        model.put(TARGET_EMAIL, email);

        @Nullable String message = null;

        switch (stateValue) {
            case ENABLED:
                model.put("apiLabel", toEnabledValueLabel(apiFeature));
                message = mergeTemplateIntoString("state.enabled.ftl", model);
                break;
            case WARNING:
                model.put("apiValueLabel", toDisabledValueLabel(apiFeature) + " " + toWarningValueLabel(msg.getKey(), msg.getValue(), msg.getThreshold()));
                message = mergeTemplateIntoString("state.warning.ftl", model);
                break;
            case DISABLED:
                model.put("apiLimitValueLabel", toDisabledValueLabel(apiFeature) + " " + toDisabledValueLabel(msg.getKey(), msg.getThreshold()));
                message = mergeTemplateIntoString("state.disabled.ftl", model);
                break;
        }
        sendMail(mailSender, mailFrom, email, subject, message, timeout);
    }

    @Override
    public void testConnection(TenantId tenantId) throws Exception {
        mailSender.testConnection();
    }

    @NotNull
    private String toEnabledValueLabel(@NotNull ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "save";
            case TRANSPORT:
                return "receive";
            case JS:
                return "invoke";
            case RE:
                return "process";
            case EMAIL:
            case SMS:
                return "send";
            case ALARM:
                return "create";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    @NotNull
    private String toDisabledValueLabel(@NotNull ApiFeature apiFeature) {
        switch (apiFeature) {
            case DB:
                return "saved";
            case TRANSPORT:
                return "received";
            case JS:
                return "invoked";
            case RE:
                return "processed";
            case EMAIL:
            case SMS:
                return "sent";
            case ALARM:
                return "created";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    @NotNull
    private String toWarningValueLabel(@NotNull ApiUsageRecordKey key, long value, long threshold) {
        String valueInM = getValueAsString(value);
        String thresholdInM = getValueAsString(threshold);
        switch (key) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed data points";
            case TRANSPORT_MSG_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed messages";
            case JS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed JavaScript functions";
            case RE_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed Email messages";
            case SMS_EXEC_COUNT:
                return valueInM + " out of " + thresholdInM + " allowed SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    @NotNull
    private String toDisabledValueLabel(@NotNull ApiUsageRecordKey key, long value) {
        switch (key) {
            case STORAGE_DP_COUNT:
            case TRANSPORT_DP_COUNT:
                return getValueAsString(value) + " data points";
            case TRANSPORT_MSG_COUNT:
                return getValueAsString(value) + " messages";
            case JS_EXEC_COUNT:
                return "JavaScript functions " + getValueAsString(value) + " times";
            case RE_EXEC_COUNT:
                return getValueAsString(value) + " Rule Engine messages";
            case EMAIL_EXEC_COUNT:
                return getValueAsString(value) + " Email messages";
            case SMS_EXEC_COUNT:
                return getValueAsString(value) + " SMS messages";
            default:
                throw new RuntimeException("Not implemented!");
        }
    }

    private String getValueAsString(long value) {
        if (value > _1M && value % _1M < _10K) {
            return value / _1M + "M";
        } else if (value > _10K) {
            return String.format("%.2fM", ((double) value) / 1000000);
        } else {
            return value + "";
        }
    }

    private void sendMail(@NotNull JavaMailSenderImpl mailSender, @NotNull String mailFrom, @NotNull String email,
                          @NotNull String subject, @NotNull String message, long timeout) throws EchoiotException {
        try {
            @NotNull MimeMessage mimeMsg = mailSender.createMimeMessage();
            @NotNull MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, UTF_8);
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(message, true);

            sendMailWithTimeout(mailSender, helper.getMimeMessage(), timeout);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private void sendMailWithTimeout(@NotNull JavaMailSender mailSender, @NotNull MimeMessage msg, long timeout) {
        try {
            mailExecutorService.submit(() -> mailSender.send(msg)).get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.debug("Error during mail submission", e);
            throw new RuntimeException("Timeout!");
        } catch (Exception e) {
            throw new RuntimeException(ExceptionUtils.getRootCause(e));
        }
    }

    @NotNull
    private String mergeTemplateIntoString(String templateLocation,
                                           @NotNull Map<String, Object> model) throws EchoiotException {
        try {
            Template template = freemarkerConfig.getTemplate(templateLocation);
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @NotNull
    protected EchoiotException handleException(Exception exception) {
        String message;
        if (exception instanceof NestedRuntimeException) {
            message = ((NestedRuntimeException) exception).getMostSpecificCause().getMessage();
        } else {
            message = exception.getMessage();
        }
        log.warn("Unable to send mail: {}", message);
        return new EchoiotException(String.format("Unable to send mail: %s", message),
                                        EchoiotErrorCode.GENERAL);
    }

}
