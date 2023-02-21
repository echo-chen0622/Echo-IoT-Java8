package org.echoiot.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.exception.DataValidationException;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.TenantEntityDao;
import org.echoiot.server.dao.TenantEntityWithDataDao;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class DataValidator<D extends BaseData<?>> {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern QUEUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+$");

    private static final String NAME = "name";
    private static final String TOPIC = "topic";

    // Returns old instance of the same object that is fetched during validation.
    @Nullable
    public D validate(@NotNull D data, @NotNull Function<D, TenantId> tenantIdFunction) {
        try {
            if (data == null) {
                throw new DataValidationException("Data object can't be null!");
            }

            ConstraintValidator.validateFields(data);

            TenantId tenantId = tenantIdFunction.apply(data);
            validateDataImpl(tenantId, data);
            @Nullable D old;
            if (data.getId() == null) {
                validateCreate(tenantId, data);
                old = null;
            } else {
                old = validateUpdate(tenantId, data);
            }
            return old;
        } catch (DataValidationException e) {
            log.error("{} object is invalid: [{}]", data == null ? "Data" : data.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    protected void validateDataImpl(TenantId tenantId, D data) {
    }

    protected void validateCreate(TenantId tenantId, D data) {
    }

    @Nullable
    protected D validateUpdate(TenantId tenantId, D data) {
        return null;
    }

    protected boolean isSameData(@NotNull D existentData, @NotNull D actualData) {
        return actualData.getId() != null && existentData.getId().equals(actualData.getId());
    }

    public static void validateEmail(String email) {
        if (!doValidateEmail(email)) {
            throw new DataValidationException("Invalid email address format '" + email + "'!");
        }
    }

    private static boolean doValidateEmail(@Nullable String email) {
        if (email == null) {
            return false;
        }

        @NotNull Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
        return emailMatcher.matches();
    }

    protected void validateNumberOfEntitiesPerTenant(TenantId tenantId,
                                                     @NotNull TenantEntityDao tenantEntityDao,
                                                     long maxEntities,
                                                     @NotNull EntityType entityType) {
        if (maxEntities > 0) {
            long currentEntitiesCount = tenantEntityDao.countByTenantId(tenantId);
            if (currentEntitiesCount >= maxEntities) {
                throw new DataValidationException(String.format("Can't create more then %d %ss!",
                        maxEntities, entityType.name().toLowerCase().replaceAll("_", " ")));
            }
        }
    }

    protected void validateMaxSumDataSizePerTenant(TenantId tenantId,
                                                   @NotNull TenantEntityWithDataDao dataDao,
                                                   long maxSumDataSize,
                                                   long currentDataSize,
                                                   @NotNull EntityType entityType) {
        if (maxSumDataSize > 0) {
            if (dataDao.sumDataSizeByTenantId(tenantId) + currentDataSize > maxSumDataSize) {
                throw new DataValidationException(String.format("Failed to create the %s, files size limit is exhausted %d bytes!",
                        entityType.name().toLowerCase().replaceAll("_", " "), maxSumDataSize));
            }
        }
    }

    protected static void validateJsonStructure(@NotNull JsonNode expectedNode, @NotNull JsonNode actualNode) {
        @NotNull Set<String> expectedFields = new HashSet<>();
        Iterator<String> fieldsIterator = expectedNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            expectedFields.add(fieldsIterator.next());
        }

        @NotNull Set<String> actualFields = new HashSet<>();
        fieldsIterator = actualNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            actualFields.add(fieldsIterator.next());
        }

        if (!expectedFields.containsAll(actualFields) || !actualFields.containsAll(expectedFields)) {
            throw new DataValidationException("Provided json structure is different from stored one '" + actualNode + "'!");
        }
    }

    protected static void validateQueueName(@NotNull String name) {
        validateQueueNameOrTopic(name, NAME);
    }

    protected static void validateQueueTopic(@NotNull String topic) {
        validateQueueNameOrTopic(topic, TOPIC);
    }

    private static void validateQueueNameOrTopic(@NotNull String value, String fieldName) {
        if (StringUtils.isEmpty(value)) {
            throw new DataValidationException(String.format("Queue %s should be specified!", fieldName));
        }
        if (!QUEUE_PATTERN.matcher(value).matches()) {
            throw new DataValidationException(
                    String.format("Queue %s contains a character other than ASCII alphanumerics, '.', '_' and '-'!", fieldName));
        }
    }

}
