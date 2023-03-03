package org.echoiot.server.dao.service;

import org.apache.commons.lang3.StringUtils;
import org.echoiot.common.util.RegexUtils;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.UUIDBased;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.query.EntityDataPageLink;
import org.echoiot.server.common.data.query.EntityKey;
import org.echoiot.server.common.data.query.EntityKeyType;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Echo
 */
public class Validator {

    /**
     * 允许 Unicode 字母、数字、“_”和“-”
     */
    public static final Pattern PROPERTY_PATTERN = Pattern.compile("^[\\p{L}0-9_-]+$");

    /**
     * 此方法验证<code>EntityId<code> ID 实体 ID。如果实体 ID 无效，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param entityId     entityId
     * @param errorMessage 异常文案
     */
    public static void validateEntityId(EntityId entityId, String errorMessage) {
        if (entityId == null || entityId.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * 此方法验证<code>String<code>字符串。如果字符串无效（为null 或空串），则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param val          校验对象
     * @param errorMessage 异常文案
     */
    public static void validateString(String val, String errorMessage) {
        if (val == null || val.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        }
    }


    /**
     * 此方法验证<code>long<code>数字。如果数字不大于0，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param val          校验对象
     * @param errorMessage 异常文案
     */
    public static void validatePositiveNumber(long val, String errorMessage) {
        if (val <= 0) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * 此方法验证 <code>UUID<code> ID。如果 id 为空，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param id           the id
     * @param errorMessage 异常文案
     */
    public static void validateId(UUID id, String errorMessage) {
        if (id == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }


    /**
     * 此方法验证 <code>UUIDBased<code> id。如果 id 为空，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param id           the id
     * @param errorMessage 异常文案
     */
    public static void validateId(UUIDBased id, String errorMessage) {
        if (id == null || id.getId() == null) {
            throw new IncorrectParameterException(errorMessage);
        }
    }

    /**
     * 此方法验证 <code>UUIDBased<code> ID 列表。如果至少有一个 id 为空，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param ids          the list of ids
     * @param errorMessage 异常文案
     */
    public static void validateIds(List<? extends UUIDBased> ids, String errorMessage) {
        if (ids == null || ids.isEmpty()) {
            throw new IncorrectParameterException(errorMessage);
        } else {
            for (UUIDBased id : ids) {
                validateId(id, errorMessage);
            }
        }
    }

    /**
     * 此方法验证页面<code>PageLink<code>链接页面链接。如果页面链接无效，则抛出
     * <code>IncorrectParameterException<code>异常
     *
     * @param pageLink the page link
     */
    public static void validatePageLink(PageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("必须指定页面链接");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("页面链接页面大小不正确 '" + pageLink.getPageSize() + "' 。页面大小必须大于零。");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("页面链接页数不正确 '" + pageLink.getPage() + "'。页数必须是正整数。");
        } else if (pageLink.getSortOrder() != null) {
            if (!isValidProperty(pageLink.getSortOrder().getProperty())) {
                throw new IncorrectParameterException("无效的页面链接排序属性");
            }
        }
    }

    @Contract("null -> fail")
    public static void validateEntityDataPageLink(EntityDataPageLink pageLink) {
        if (pageLink == null) {
            throw new IncorrectParameterException("Entity Data Page link must be specified.");
        } else if (pageLink.getPageSize() < 1) {
            throw new IncorrectParameterException("Incorrect entity data page link page size '" + pageLink.getPageSize() + "'. Page size must be greater than zero.");
        } else if (pageLink.getPage() < 0) {
            throw new IncorrectParameterException("Incorrect entity data page link page '" + pageLink.getPage() + "'. Page must be positive integer.");
        } else if (pageLink.getSortOrder() != null && pageLink.getSortOrder().getKey() != null) {
            EntityKey sortKey = pageLink.getSortOrder().getKey();
            if ((sortKey.getType() == EntityKeyType.ENTITY_FIELD || sortKey.getType() == EntityKeyType.ALARM_FIELD) && !isValidProperty(sortKey.getKey())) {
                throw new IncorrectParameterException("Invalid entity data page link sort property");
            }
        }
    }

    public static boolean isValidProperty(String key) {
        return StringUtils.isEmpty(key) || RegexUtils.matches(key, PROPERTY_PATTERN);
    }

}
