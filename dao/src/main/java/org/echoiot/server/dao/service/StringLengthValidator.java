package org.echoiot.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.validation.Length;
import org.jetbrains.annotations.NotNull;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class StringLengthValidator implements ConstraintValidator<Length, String> {
    private int max;

    @Override
    public boolean isValid(@NotNull String value, ConstraintValidatorContext context) {
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
