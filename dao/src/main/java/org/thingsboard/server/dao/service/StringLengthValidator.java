package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.Length;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
    public void initialize(Length constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }
}
