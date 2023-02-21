package org.echoiot.server.service.edge.rpc.fetch;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.WordUtils;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.common.data.AdminSettings;
import org.echoiot.server.common.data.EdgeUtils;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.edge.Edge;
import org.echoiot.server.common.data.edge.EdgeEvent;
import org.echoiot.server.common.data.edge.EdgeEventActionType;
import org.echoiot.server.common.data.edge.EdgeEventType;
import org.echoiot.server.common.data.id.AdminSettingsId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.dao.settings.AdminSettingsService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@Slf4j
public class AdminSettingsEdgeEventFetcher implements EdgeEventFetcher {

    @NotNull
    private final AdminSettingsService adminSettingsService;
    @NotNull
    private final Configuration freemarkerConfig;

    private static final Pattern startPattern = Pattern.compile("<div class=\"content\".*?>");
    private static final Pattern endPattern = Pattern.compile("<div class=\"footer\".*?>");

    private static final List<String> templatesNames = Arrays.asList(
            "account.activated.ftl",
            "account.lockout.ftl",
            "activation.ftl",
            "password.was.reset.ftl",
            "reset.password.ftl",
            "test.ftl");

    // TODO: @voba fix format of next templates
    // "state.disabled.ftl",
    // "state.enabled.ftl",
    // "state.warning.ftl",

    @Nullable
    @Override
    public PageLink getPageLink(int pageSize) {
        return null;
    }

    @NotNull
    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, @NotNull Edge edge, PageLink pageLink) throws Exception {
        @NotNull List<EdgeEvent> result = new ArrayList<>();

        AdminSettings systemMailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                                                EdgeEventActionType.UPDATED, null, JacksonUtil.OBJECT_MAPPER.valueToTree(systemMailSettings)));

        @NotNull AdminSettings tenantMailSettings = convertToTenantAdminSettings(tenantId, systemMailSettings.getKey(), (ObjectNode) systemMailSettings.getJsonValue());
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                EdgeEventActionType.UPDATED, null, JacksonUtil.OBJECT_MAPPER.valueToTree(tenantMailSettings)));

        @NotNull AdminSettings systemMailTemplates = loadMailTemplates();
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                EdgeEventActionType.UPDATED, null, JacksonUtil.OBJECT_MAPPER.valueToTree(systemMailTemplates)));

        @NotNull AdminSettings tenantMailTemplates = convertToTenantAdminSettings(tenantId, systemMailTemplates.getKey(), (ObjectNode) systemMailTemplates.getJsonValue());
        result.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.ADMIN_SETTINGS,
                EdgeEventActionType.UPDATED, null, JacksonUtil.OBJECT_MAPPER.valueToTree(tenantMailTemplates)));

        // return PageData object to be in sync with other fetchers
        return new PageData<>(result, 1, result.size(), false);
    }

    @NotNull
    private AdminSettings loadMailTemplates() throws Exception {
        @NotNull Map<String, Object> mailTemplates = new HashMap<>();
        for (String templatesName : templatesNames) {
            Template template = freemarkerConfig.getTemplate(templatesName);
            if (template != null) {
                @NotNull String name = validateName(template.getName());
                @Nullable Map<String, String> mailTemplate = getMailTemplateFromFile(template.toString());
                if (mailTemplate != null) {
                    mailTemplates.put(name, mailTemplate);
                } else {
                    log.error("Can't load mail template from file {}", template.getName());
                }
            }
        }
        @NotNull AdminSettings adminSettings = new AdminSettings();
        adminSettings.setId(new AdminSettingsId(Uuids.timeBased()));
        adminSettings.setKey("mailTemplates");
        adminSettings.setJsonValue(JacksonUtil.OBJECT_MAPPER.convertValue(mailTemplates, JsonNode.class));
        return adminSettings;
    }

    @Nullable
    private Map<String, String> getMailTemplateFromFile(@NotNull String stringTemplate) {
        @NotNull Map<String, String> mailTemplate = new HashMap<>();
        @NotNull Matcher start = startPattern.matcher(stringTemplate);
        @NotNull Matcher end = endPattern.matcher(stringTemplate);
        if (start.find() && end.find()) {
            @NotNull String body = StringUtils.substringBetween(stringTemplate, start.group(), end.group()).replaceAll("\t", "");
            String subject = StringUtils.substringBetween(body, "<h2>", "</h2>");
            mailTemplate.put("subject", subject);
            mailTemplate.put("body", body);
        } else {
            return null;
        }
        return mailTemplate;
    }

    @NotNull
    private String validateName(String name) throws Exception {
        @NotNull StringBuilder nameBuilder = new StringBuilder();
        name = name.replace(".ftl", "");
        @NotNull String[] nameParts = name.split("\\.");
        if (nameParts.length >= 1) {
            nameBuilder.append(nameParts[0]);
            for (int i = 1; i < nameParts.length; i++) {
                @Nullable String word = WordUtils.capitalize(nameParts[i]);
                nameBuilder.append(word);
            }
            return nameBuilder.toString();
        } else {
            throw new Exception("Error during filename validation");
        }
    }

    @NotNull
    private AdminSettings convertToTenantAdminSettings(TenantId tenantId, String key, @NotNull ObjectNode jsonValue) {
        @NotNull AdminSettings tenantMailSettings = new AdminSettings();
        tenantMailSettings.setTenantId(tenantId);
        jsonValue.put("useSystemMailSettings", true);
        tenantMailSettings.setJsonValue(jsonValue);
        tenantMailSettings.setKey(key);
        return tenantMailSettings;
    }
}
