package org.echoiot.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.echoiot.rule.engine.api.MailService;
import org.echoiot.rule.engine.api.SmsService;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.UpdateMessage;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.security.model.JwtPair;
import org.echoiot.server.common.data.security.model.JwtSettings;
import org.echoiot.server.common.data.security.model.SecuritySettings;
import org.echoiot.server.common.data.sms.config.TestSmsRequest;
import org.echoiot.server.common.data.sync.vc.AutoCommitSettings;
import org.echoiot.server.common.data.sync.vc.RepositorySettings;
import org.echoiot.server.common.data.sync.vc.RepositorySettingsInfo;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.token.JwtTokenFactory;
import org.echoiot.server.service.security.permission.Operation;
import org.echoiot.server.service.security.permission.Resource;
import org.echoiot.server.service.security.system.SystemSecurityService;
import org.echoiot.server.service.sync.vc.EntitiesVersionControlService;
import org.echoiot.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.echoiot.server.service.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@TbCoreComponent
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    @Autowired
    private MailService mailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private SystemSecurityService systemSecurityService;

    @Lazy
    @Autowired
    private JwtSettingsService jwtSettingsService;

    @Lazy
    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private EntitiesVersionControlService versionControlService;

    @Autowired
    private TbAutoCommitSettingsService autoCommitSettingsService;

    @Autowired
    private UpdateService updateService;

    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Get the Administration Settings object using specified string key. Referencing non-existing key will cause an error." + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/{key}", method = RequestMethod.GET)
    @ResponseBody
    public AdminSettings getAdminSettings(
            @ApiParam(value = "A string value of the key (e.g. 'general' or 'mail').")
            @PathVariable("key") String key) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            AdminSettings adminSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, key), "No Administration settings found for key: " + key);
            if (adminSettings.getKey().equals("mail")) {
                ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            }
            return adminSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Get the Administration Settings object using key (getAdminSettings)",
            notes = "Creates or Updates the Administration Settings. Platform generates random Administration Settings Id during settings creation. " +
                    "The Administration Settings Id will be present in the response. Specify the Administration Settings Id when you would like to update the Administration Settings. " +
                    "Referencing non-existing Administration Settings Id will cause an error." + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings", method = RequestMethod.POST)
    @ResponseBody
    public AdminSettings saveAdminSettings(
            @ApiParam(value = "A JSON value representing the Administration Settings.")
            @RequestBody AdminSettings adminSettings) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
            adminSettings.setTenantId(getTenantId());
            adminSettings = checkNotNull(adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, adminSettings));
            if (adminSettings.getKey().equals("mail")) {
                mailService.updateMailConfiguration();
                ((ObjectNode) adminSettings.getJsonValue()).remove("password");
            } else if (adminSettings.getKey().equals("sms")) {
                smsService.updateSmsConfiguration();
            }
            return adminSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the Security Settings object",
            notes = "Get the Security Settings object that contains password policy, etc." + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.GET)
    @ResponseBody
    public SecuritySettings getSecuritySettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            return checkNotNull(systemSecurityService.getSecuritySettings(TenantId.SYS_TENANT_ID));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update Security Settings (saveSecuritySettings)",
            notes = "Updates the Security Settings object that contains password policy, etc." + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/securitySettings", method = RequestMethod.POST)
    @ResponseBody
    public SecuritySettings saveSecuritySettings(
            @ApiParam(value = "A JSON value representing the Security Settings.")
            @RequestBody SecuritySettings securitySettings) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.WRITE);
            securitySettings = checkNotNull(systemSecurityService.saveSecuritySettings(TenantId.SYS_TENANT_ID, securitySettings));
            return securitySettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get the JWT Settings object (getJwtSettings)",
            notes = "Get the JWT Settings object that contains JWT token policy, etc. " + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.GET)
    @ResponseBody
    public JwtSettings getJwtSettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            return checkNotNull(jwtSettingsService.getJwtSettings());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Update JWT Settings (saveJwtSettings)",
            notes = "Updates the JWT Settings object that contains JWT token policy, etc. The tokenSigningKey field is a Base64 encoded string." + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/jwtSettings", method = RequestMethod.POST)
    @ResponseBody
    public JwtPair saveJwtSettings(
            @ApiParam(value = "A JSON value representing the JWT Settings.")
            @RequestBody JwtSettings jwtSettings) throws EchoiotException {
        try {
            SecurityUser securityUser = getCurrentUser();
            accessControlService.checkPermission(securityUser, Resource.ADMIN_SETTINGS, Operation.WRITE);
            checkNotNull(jwtSettingsService.saveJwtSettings(jwtSettings));
            return tokenFactory.createTokenPair(securityUser);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Send test email (sendTestMail)",
            notes = "Attempts to send test email to the System Administrator User using Mail Settings provided as a parameter. " +
                    "You may change the 'To' email in the user profile of the System Administrator. " + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testMail", method = RequestMethod.POST)
    public void sendTestMail(
            @ApiParam(value = "A JSON value representing the Mail Settings.")
            @RequestBody AdminSettings adminSettings) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            adminSettings = checkNotNull(adminSettings);
            if (adminSettings.getKey().equals("mail")) {
                if (!adminSettings.getJsonValue().has("password")) {
                    AdminSettings mailSettings = checkNotNull(adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail"));
                    ((ObjectNode) adminSettings.getJsonValue()).put("password", mailSettings.getJsonValue().get("password").asText());
                }
                String email = getCurrentUser().getEmail();
                mailService.sendTestMail(adminSettings.getJsonValue(), email);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Send test sms (sendTestMail)",
            notes = "Attempts to send test sms to the System Administrator User using SMS Settings and phone number provided as a parameters of the request. "
                    + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/settings/testSms", method = RequestMethod.POST)
    public void sendTestSms(
            @ApiParam(value = "A JSON value representing the Test SMS request.")
            @RequestBody TestSmsRequest testSmsRequest) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.ADMIN_SETTINGS, Operation.READ);
            smsService.sendTestSms(testSmsRequest);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get repository settings (getRepositorySettings)",
            notes = "Get the repository settings object. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings")
    public RepositorySettings getRepositorySettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            RepositorySettings versionControlSettings = checkNotNull(versionControlService.getVersionControlSettings(getTenantId()));
            versionControlSettings.setPassword(null);
            versionControlSettings.setPrivateKey(null);
            versionControlSettings.setPrivateKeyPassword(null);
            return versionControlSettings;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check repository settings exists (repositorySettingsExists)",
            notes = "Check whether the repository settings exists. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/exists")
    public Boolean repositorySettingsExists() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return versionControlService.getVersionControlSettings(getTenantId()) != null;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/repositorySettings/info")
    public RepositorySettingsInfo getRepositorySettingsInfo() throws Exception {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
        RepositorySettings repositorySettings = versionControlService.getVersionControlSettings(getTenantId());
        if (repositorySettings != null) {
            return RepositorySettingsInfo.builder()
                    .configured(true)
                    .readOnly(repositorySettings.isReadOnly())
                    .build();
        } else {
            return RepositorySettingsInfo.builder()
                    .configured(false)
                    .build();
        }
    }

    @ApiOperation(value = "Creates or Updates the repository settings (saveRepositorySettings)",
            notes = "Creates or Updates the repository settings object. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/repositorySettings")
    public DeferredResult<RepositorySettings> saveRepositorySettings(@RequestBody RepositorySettings settings) throws EchoiotException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        ListenableFuture<RepositorySettings> future = versionControlService.saveVersionControlSettings(getTenantId(), settings);
        return wrapFuture(Futures.transform(future, savedSettings -> {
            savedSettings.setPassword(null);
            savedSettings.setPrivateKey(null);
            savedSettings.setPrivateKeyPassword(null);
            return savedSettings;
        }, MoreExecutors.directExecutor()));
    }

    @ApiOperation(value = "Delete repository settings (deleteRepositorySettings)",
            notes = "Deletes the repository settings."
                    + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public DeferredResult<Void> deleteRepositorySettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
            return wrapFuture(versionControlService.deleteVersionControlSettings(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    @ApiOperation(value = "Check repository access (checkRepositoryAccess)",
            notes = "Attempts to check repository access. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/repositorySettings/checkAccess", method = RequestMethod.POST)
    public DeferredResult<Void> checkRepositoryAccess(
            @ApiParam(value = "A JSON value representing the Repository Settings.")
            @RequestBody RepositorySettings settings) throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            settings = checkNotNull(settings);
            return wrapFuture(versionControlService.checkVersionControlAccess(getTenantId(), settings));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Get auto commit settings (getAutoCommitSettings)",
            notes = "Get the auto commit settings object. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings")
    public AutoCommitSettings getAutoCommitSettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return checkNotNull(autoCommitSettingsService.get(getTenantId()));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check auto commit settings exists (autoCommitSettingsExists)",
            notes = "Check whether the auto commit settings exists. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping("/autoCommitSettings/exists")
    public Boolean autoCommitSettingsExists() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.READ);
            return autoCommitSettingsService.get(getTenantId()) != null;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Creates or Updates the auto commit settings (saveAutoCommitSettings)",
            notes = "Creates or Updates the auto commit settings object. " + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping("/autoCommitSettings")
    public AutoCommitSettings saveAutoCommitSettings(@RequestBody AutoCommitSettings settings) throws EchoiotException {
        accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.WRITE);
        return autoCommitSettingsService.save(getTenantId(), settings);
    }

    @ApiOperation(value = "Delete auto commit settings (deleteAutoCommitSettings)",
            notes = "Deletes the auto commit settings."
                    + ControllerConstants.TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/autoCommitSettings", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteAutoCommitSettings() throws EchoiotException {
        try {
            accessControlService.checkPermission(getCurrentUser(), Resource.VERSION_CONTROL, Operation.DELETE);
            autoCommitSettingsService.delete(getTenantId());
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @ApiOperation(value = "Check for new Platform Releases (checkUpdates)",
            notes = "Check notifications about new platform releases. "
                    + ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/updates", method = RequestMethod.GET)
    @ResponseBody
    public UpdateMessage checkUpdates() throws EchoiotException {
        try {
            return updateService.checkUpdates();
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}