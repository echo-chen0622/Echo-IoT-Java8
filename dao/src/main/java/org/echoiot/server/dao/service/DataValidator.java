package org.echoiot.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.TenantEntityDao;
import org.echoiot.server.dao.TenantEntityWithDataDao;
import org.echoiot.server.dao.exception.DataValidationException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据校验器
 */
@Slf4j
public abstract class DataValidator<D extends BaseData<?>> {
    // EMAIL 校验正则表达式
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    // 队列 校验正则表达式
    private static final Pattern QUEUE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.\\-]+$");

    private static final String NAME = "name";
    private static final String TOPIC = "topic";

    /**
     * 校验数据
     *
     * @return 在验证期间提取的同一对象的旧实例
     */
    @Nullable
    public D validate(D data, Function<D, TenantId> tenantIdFunction) {
        try {
            if (data == null) {
                throw new DataValidationException("数据对象不能为空！");
            }
            // 调用 hibernate 进行校验数据，通用校验，所有租户都必须满足的规则
            ConstraintValidator.validateFields(data);

            // 获取租户，并进行租户独有的校验。各租户可自定义校验规则
            TenantId tenantId = tenantIdFunction.apply(data);
            // 租户数据校验
            validateDataImpl(tenantId, data);
            @Nullable D old;
            if (data.getId() == null) {
                // 创建数据时的租户校验
                validateCreate(tenantId, data);
                old = null;
            } else {
                // 更新数据时的租户校验
                old = validateUpdate(tenantId, data);
            }
            // 返回历史数据
            return old;
        } catch (DataValidationException e) {
            // 数据校验异常，直接抛出，并打印校验错误信息
            log.error("{} 对象无效: [{}]", data == null ? "Data" : data.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    /**
     * 租户校验数据
     */
    protected void validateDataImpl(TenantId tenantId, D data) {
    }

    /**
     * 创建数据时的租户校验
     */
    protected void validateCreate(TenantId tenantId, D data) {
    }

    /**
     * 更新数据时的租户校验
     */
    @Nullable
    protected D validateUpdate(TenantId tenantId, D data) {
        return null;
    }

    /**
     * 幂等校验---租户自定义幂等规则
     */
    protected boolean isSameData(D existentData, D actualData) {
        return actualData.getId() != null && existentData.getId().equals(actualData.getId());
    }

    /**
     * 校验邮箱
     */
    public static void validateEmail(String email) {
        if (!doValidateEmail(email)) {
            throw new DataValidationException("电子邮件地址格式无效 '" + email + "'!");
        }
    }

    /**
     * 校验邮箱
     */
    @Contract("null -> false")
    private static boolean doValidateEmail(@Nullable String email) {
        if (email == null) {
            return false;
        }

        Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
        return emailMatcher.matches();
    }

    /**
     * 验证每个租户的实体数
     */
    protected void validateNumberOfEntitiesPerTenant(TenantId tenantId, TenantEntityDao tenantEntityDao, long maxEntities, EntityType entityType) {
        if (maxEntities > 0) {
            // 获取当前租户的实体数
            long currentEntitiesCount = tenantEntityDao.countByTenantId(tenantId);
            if (currentEntitiesCount >= maxEntities) {
                // 如果当前租户的实体数大于等于最大实体数，则抛出异常
                throw new DataValidationException(String.format("无法创建更多 %d %ss!", maxEntities, entityType.name().toLowerCase().replaceAll("_", " ")));
            }
        }
    }

    /**
     * 验证每个租户的实体数据总大小
     */
    protected void validateMaxSumDataSizePerTenant(TenantId tenantId, TenantEntityWithDataDao dataDao, long maxSumDataSize, long currentDataSize, EntityType entityType) {
        if (maxSumDataSize > 0) {
            // 获取并验证当前租户的实体数据总大小
            if (dataDao.sumDataSizeByTenantId(tenantId) + currentDataSize > maxSumDataSize) {
                throw new DataValidationException(String.format("无法创建 %s, 文件大小限制已用尽 %d bytes!", entityType.name().toLowerCase().replaceAll("_", " "), maxSumDataSize));
            }
        }
    }

    /**
     * JSON 结构校验
     * 校验两个 JSON 结构是否相同，只校验属性名
     *
     * @param expectedNode 期望的 JSON 结构
     * @param actualNode   实际的 JSON 结构
     */
    protected static void validateJsonStructure(@NotNull JsonNode expectedNode, JsonNode actualNode) {
        Set<String> expectedFields = new HashSet<>();
        // 获取所有字段名
        Iterator<String> fieldsIterator = expectedNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            expectedFields.add(fieldsIterator.next());
        }

        Set<String> actualFields = new HashSet<>();
        // 获取所有字段名
        fieldsIterator = actualNode.fieldNames();
        while (fieldsIterator.hasNext()) {
            actualFields.add(fieldsIterator.next());
        }

        // 校验两个 JSON 结构是否相同。只校验属性名
        if (expectedFields.size() != actualFields.size() || !actualFields.containsAll(expectedFields)) {
            throw new DataValidationException("提供的 json 结构与期望的结构不同 '" + actualNode + "'!");
        }
    }

    /**
     * 校验队列名称
     */
    protected static void validateQueueName(String name) {
        validateQueueNameOrTopic(name, NAME);
    }

    /**
     * 校验队列主题
     */
    protected static void validateQueueTopic(String topic) {
        validateQueueNameOrTopic(topic, TOPIC);
    }

    /**
     * 校验队列名称或主题
     *
     * @param value     队列名称或主题
     * @param fieldName 字段名
     */
    private static void validateQueueNameOrTopic(String value, String fieldName) {
        if (StringUtils.isEmpty(value)) {
            throw new DataValidationException(String.format("Queue %s 应指定!", fieldName));
        }
        if (!QUEUE_PATTERN.matcher(value).matches()) {
            throw new DataValidationException(String.format("Queue %s 包含 ASCII 字母数字, '.', '_' 和 '-'以外的字符!", fieldName));
        }
    }

}
