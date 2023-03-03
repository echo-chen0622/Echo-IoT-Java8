package org.echoiot.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.NotNull;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 字符串最大长度校验器
 * 定义次校验器，而不用 Hibernate 的校验器，是因为 Hibernate 的校验器不支持字符串
 */
@Slf4j
public class StringLengthValidator implements ConstraintValidator<Length, String> {
    private int max;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        return value.length() <= max;
    }

    @Override
    public void initialize(@NotNull Length constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }
}
